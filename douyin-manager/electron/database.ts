import initSqlJs, { Database } from 'sql.js'
import mysql from 'mysql2/promise'
import log from 'electron-log'
import { app } from 'electron'
import path from 'path'
import fs from 'fs'

// Remote database - MySQL
const remoteConfig = {
  host: 'your_mysql_host',
  port: 3310,
  user: 'your_username',
  password: 'your_password',
  database: 'your_username'
}

let remotePool: mysql.Pool | null = null
let localDb: Database | null = null
let dbPath: string = ''
let isInitialized = false

// 保存数据库到文件
function saveDatabase() {
  if (localDb && dbPath) {
    const data = localDb.export()
    const buffer = Buffer.from(data)
    fs.writeFileSync(dbPath, buffer)
  }
}

export async function initDatabases() {
  // 先初始化本地数据库
  try {
    log.info('Initializing local database connection (SQLite with sql.js)...')
    
    const userDataPath = app.getPath('userData')
    dbPath = path.join(userDataPath, 'douyin_stats.db')
    log.info(`SQLite database path: ${dbPath}`)
    
    const wasmPath = path.join(__dirname, '../node_modules/sql.js/dist/sql-wasm.wasm')
    const wasmBinary = fs.readFileSync(wasmPath)
    
    const SQL = await initSqlJs({
      wasmBinary: wasmBinary
    })

    if (fs.existsSync(dbPath)) {
      const buffer = fs.readFileSync(dbPath)
      localDb = new SQL.Database(buffer)
      log.info('Loaded existing SQLite database')
    } else {
      localDb = new SQL.Database()
      log.info('Created new SQLite database')
    }

    // Create local tables
    localDb.run(`
      CREATE TABLE IF NOT EXISTS anchors (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        anchor_id TEXT UNIQUE NOT NULL,
        anchor_name TEXT,
        serial_number INTEGER DEFAULT 0,
        gender TEXT DEFAULT 'male',
        douyin_no TEXT,
        huoshan_no TEXT,
        created_at TEXT DEFAULT (datetime('now', 'localtime')),
        updated_at TEXT DEFAULT (datetime('now', 'localtime'))
      )
    `)

    localDb.run(`
      CREATE TABLE IF NOT EXISTS wave_stats (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        anchor_id TEXT NOT NULL,
        date TEXT NOT NULL,
        wave_value INTEGER DEFAULT 0,
        rank INTEGER DEFAULT 0,
        created_at TEXT DEFAULT (datetime('now', 'localtime')),
        UNIQUE (anchor_id, date)
      )
    `)

    localDb.run(`
      CREATE TABLE IF NOT EXISTS duration_stats (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        anchor_id TEXT NOT NULL,
        date TEXT NOT NULL,
        duration_minutes INTEGER DEFAULT 0,
        created_at TEXT DEFAULT (datetime('now', 'localtime')),
        UNIQUE (anchor_id, date)
      )
    `)

    try {
      localDb.run('CREATE INDEX IF NOT EXISTS idx_wave_stats_date ON wave_stats(date)')
      localDb.run('CREATE INDEX IF NOT EXISTS idx_duration_stats_date ON duration_stats(date)')
      localDb.run('CREATE INDEX IF NOT EXISTS idx_anchors_serial ON anchors(serial_number)')
    } catch (e) {}

    saveDatabase()
    log.info('Local SQLite tables created/verified')
    isInitialized = true
  } catch (error) {
    log.error('Local database initialization failed:', error)
    throw error
  }

  // 尝试连接远程数据库
  try {
    log.info('Initializing remote database connection...')
    remotePool = mysql.createPool(remoteConfig)
    await remotePool.query('SELECT 1')
    log.info('Remote database connection successful')

    // Create remote tables
    await remotePool.execute(`
      CREATE TABLE IF NOT EXISTS wave_stats (
        id INT AUTO_INCREMENT PRIMARY KEY,
        anchor_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
        date DATE NOT NULL,
        wave_value BIGINT DEFAULT 0,
        \`rank\` INT DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY unique_anchor_date (anchor_id, date)
      ) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
    `)

    await remotePool.execute(`
      CREATE TABLE IF NOT EXISTS duration_stats (
        id INT AUTO_INCREMENT PRIMARY KEY,
        anchor_id VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
        date DATE NOT NULL,
        duration_minutes INT DEFAULT 0,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY unique_anchor_date (anchor_id, date)
      ) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
    `)
    log.info('Remote database tables verified')
  } catch (e) {
    log.warn('Remote database connection failed:', e)
    remotePool = null
  }

  log.info('Database initialization completed')
}

export async function syncAnchors() {
  if (!remotePool || !localDb) return

  try {
    const [rows] = await remotePool.query<any>(
      'SELECT streamer_id, nickname, serial_number, gender FROM user WHERE streamer_id IS NOT NULL AND streamer_id != ""'
    )
    log.info(`Found ${rows.length} anchors from remote database`)

    for (const anchor of rows) {
      if (!anchor.streamer_id) continue
      try {
        localDb.run(`
          INSERT INTO anchors (anchor_id, anchor_name, serial_number, gender)
          VALUES (?, ?, ?, ?)
          ON CONFLICT(anchor_id) DO UPDATE SET 
            anchor_name = excluded.anchor_name, 
            serial_number = excluded.serial_number, 
            gender = excluded.gender
        `, [String(anchor.streamer_id), anchor.nickname || '', anchor.serial_number || 0, anchor.gender || 'male'])
      } catch (e) {}
    }
    saveDatabase()
    log.info('Anchors synced successfully')
  } catch (e) {
    log.warn('Failed to sync anchors:', e)
  }
}

export async function syncWaveStats() {
  if (!remotePool || !localDb) return { count: 0 }

  try {
    const [rows] = await remotePool.query<any[]>('SELECT anchor_id, date, wave_value, `rank` FROM wave_stats')
    log.info(`Found ${rows.length} wave stats from remote`)

    for (const item of rows) {
      const dateStr = item.date instanceof Date
        ? `${item.date.getFullYear()}-${String(item.date.getMonth() + 1).padStart(2, '0')}-${String(item.date.getDate()).padStart(2, '0')}`
        : String(item.date)
      try {
        localDb.run(`
          INSERT INTO wave_stats (anchor_id, date, wave_value, rank)
          VALUES (?, ?, ?, ?)
          ON CONFLICT(anchor_id, date) DO UPDATE SET wave_value = excluded.wave_value, rank = excluded.rank
        `, [item.anchor_id, dateStr, item.wave_value || 0, item.rank || 0])
      } catch (e) {}
    }
    saveDatabase()
    log.info(`Synced ${rows.length} wave stats to local`)
    return { count: rows.length }
  } catch (e) {
    log.warn('Failed to sync wave stats:', e)
    return { count: 0 }
  }
}

export async function syncDurationStats() {
  if (!remotePool || !localDb) return { count: 0 }

  try {
    const [rows] = await remotePool.query<any[]>('SELECT anchor_id, date, duration_minutes FROM duration_stats')
    log.info(`Found ${rows.length} duration stats from remote`)

    for (const item of rows) {
      const dateStr = item.date instanceof Date
        ? `${item.date.getFullYear()}-${String(item.date.getMonth() + 1).padStart(2, '0')}-${String(item.date.getDate()).padStart(2, '0')}`
        : String(item.date)
      try {
        localDb.run(`
          INSERT INTO duration_stats (anchor_id, date, duration_minutes)
          VALUES (?, ?, ?)
          ON CONFLICT(anchor_id, date) DO UPDATE SET duration_minutes = excluded.duration_minutes
        `, [item.anchor_id, dateStr, item.duration_minutes || 0])
      } catch (e) {}
    }
    saveDatabase()
    log.info(`Synced ${rows.length} duration stats to local`)
    return { count: rows.length }
  } catch (e) {
    log.warn('Failed to sync duration stats:', e)
    return { count: 0 }
  }
}

export async function syncAllData() {
  if (!remotePool) {
    log.info('No remote connection, skip sync')
    return { anchors: 0, waveStats: 0, durationStats: 0 }
  }

  log.info('Starting full data sync from remote to local...')
  
  await syncAnchors()
  const waveResult = await syncWaveStats()
  const durationResult = await syncDurationStats()
  
  log.info('Full data sync completed')
  return { 
    anchors: 1, 
    waveStats: waveResult.count, 
    durationStats: durationResult.count 
  }
}

export async function getAnchors() {
  if (!localDb) throw new Error('Local database not initialized')

  const result = localDb.exec('SELECT * FROM anchors ORDER BY serial_number ASC, anchor_name ASC')
  if (result.length === 0) return []
  
  const columns = result[0].columns
  return result[0].values.map((row: any[]) => {
    const obj: any = {}
    columns.forEach((col: string, i: number) => { obj[col] = row[i] })
    return obj
  })
}

export async function getNextSerialNumber() {
  if (!localDb) throw new Error('Local database not initialized')

  const result = localDb.exec('SELECT MAX(serial_number) as max_serial FROM anchors')
  if (result.length === 0 || result[0].values.length === 0) return 1
  return (result[0].values[0][0] as number | null || 0) + 1
}

export interface AnchorData {
  anchor_id: string
  anchor_name: string
  serial_number?: number
  gender?: 'male' | 'female'
}

export async function addAnchor(data: AnchorData) {
  if (!localDb) throw new Error('Local database not initialized')

  localDb.run(`INSERT INTO anchors (anchor_id, anchor_name, serial_number, gender) VALUES (?, ?, ?, ?)`,
    [data.anchor_id, data.anchor_name || '', data.serial_number || 0, data.gender || 'male'])
  saveDatabase()

  // 同步到远程
  if (remotePool) {
    try {
      await remotePool.query(
        `INSERT INTO user (streamer_id, nickname, serial_number, gender) VALUES (?, ?, ?, ?)
         ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), serial_number = VALUES(serial_number), gender = VALUES(gender)`,
        [data.anchor_id, data.anchor_name || '', data.serial_number || 0, data.gender || 'male']
      )
      log.info('Anchor synced to remote')
    } catch (e) {
      log.warn('Failed to sync anchor to remote:', e)
    }
  }
  log.info(`Added anchor: ${data.anchor_id}`)
}

export async function updateAnchor(id: number, data: Partial<AnchorData>) {
  if (!localDb) throw new Error('Local database not initialized')

  // 获取原始的anchor_id，用于后续更新关联数据
  const originalResult = localDb.exec('SELECT anchor_id FROM anchors WHERE id = ?', [id])
  const originalAnchorId = originalResult.length > 0 && originalResult[0].values.length > 0 ? originalResult[0].values[0][0] as string : null

  const updates: string[] = []
  const values: any[] = []
  let newAnchorId = originalAnchorId

  if (data.anchor_id !== undefined) { 
    updates.push('anchor_id = ?'); 
    values.push(data.anchor_id); 
    newAnchorId = data.anchor_id;
  }
  if (data.anchor_name !== undefined) { updates.push('anchor_name = ?'); values.push(data.anchor_name) }
  if (data.serial_number !== undefined) { updates.push('serial_number = ?'); values.push(data.serial_number) }
  if (data.gender !== undefined) { updates.push('gender = ?'); values.push(data.gender) }
  if (updates.length === 0) return

  values.push(id)
  localDb.run(`UPDATE anchors SET ${updates.join(', ')} WHERE id = ?`, values)

  // 如果ID发生了变化，需要更新所有关联的统计数据
  if (originalAnchorId && originalAnchorId !== newAnchorId) {
    // 更新音浪统计数据
    localDb.run(`UPDATE wave_stats SET anchor_id = ? WHERE anchor_id = ?`, [newAnchorId, originalAnchorId])
    // 更新时长统计数据
    localDb.run(`UPDATE duration_stats SET anchor_id = ? WHERE anchor_id = ?`, [newAnchorId, originalAnchorId])
  }

  saveDatabase()
  log.info(`Updated anchor id=${id}, old_anchor_id=${originalAnchorId}, new_anchor_id=${newAnchorId}`)

  // 同步到远程数据库
  if (remotePool && originalAnchorId) {
    try {
      if (originalAnchorId !== newAnchorId) {
        // 如果ID改变了，需要删除旧记录并插入新记录
        await remotePool.query('DELETE FROM user WHERE streamer_id = ?', [originalAnchorId])
        await remotePool.query(
          `INSERT INTO user (streamer_id, nickname, serial_number, gender) VALUES (?, ?, ?, ?)
           ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), serial_number = VALUES(serial_number), gender = VALUES(gender)`,
          [newAnchorId, data.anchor_name || '', data.serial_number || 0, data.gender || 'male']
        )
        // 同步更新关联的统计数据表中的anchor_id
        await remotePool.query('UPDATE wave_stats SET anchor_id = ? WHERE anchor_id = ?', [newAnchorId, originalAnchorId])
        await remotePool.query('UPDATE duration_stats SET anchor_id = ? WHERE anchor_id = ?', [newAnchorId, originalAnchorId])
      } else {
        // ID没有改变，只需更新其他字段
        await remotePool.query(
          `UPDATE user SET nickname = ?, serial_number = ?, gender = ? WHERE streamer_id = ?`,
          [data.anchor_name || '', data.serial_number || 0, data.gender || 'male', originalAnchorId]
        )
      }
      log.info('Anchor synced to remote')
    } catch (e) {
      log.warn('Failed to sync anchor to remote:', e)
    }
  }
}

export async function deleteAnchor(id: number) {
  if (!localDb) throw new Error('Local database not initialized')

  const result = localDb.exec('SELECT anchor_id FROM anchors WHERE id = ?', [id])
  const anchorId = result.length > 0 && result[0].values.length > 0 ? result[0].values[0][0] as string : null

  localDb.run('DELETE FROM anchors WHERE id = ?', [id])
  saveDatabase()

  if (remotePool && anchorId) {
    try {
      await remotePool.query('DELETE FROM user WHERE streamer_id = ?', [anchorId])
      log.info('Anchor deleted from remote')
    } catch (e) {
      log.warn('Failed to delete anchor from remote:', e)
    }
  }
  log.info(`Deleted anchor id=${id}`)
}

export async function addWaveStats(data: { anchor_id: string; date: string; wave_value: number; rank: number }[]) {
  if (!localDb) throw new Error('Local database not initialized')
  log.info(`Adding ${data.length} wave stats to local...`)

  for (const item of data) {
    localDb.run(`INSERT INTO wave_stats (anchor_id, date, wave_value, rank) VALUES (?, ?, ?, ?)
      ON CONFLICT(anchor_id, date) DO UPDATE SET wave_value = excluded.wave_value, rank = excluded.rank`,
      [item.anchor_id, item.date, item.wave_value, item.rank])
  }
  saveDatabase()
  log.info(`Added ${data.length} wave stats to local`)

  // 后台同步到远程
  if (remotePool) {
    setImmediate(async () => {
      let success = 0
      for (const item of data) {
        try {
          await remotePool!.query(
            `INSERT INTO wave_stats (anchor_id, date, wave_value, \`rank\`) VALUES (?, ?, ?, ?)
             ON DUPLICATE KEY UPDATE wave_value = VALUES(wave_value), \`rank\` = VALUES(\`rank\`)`,
            [item.anchor_id, item.date, item.wave_value, item.rank])
          success++
        } catch (e) {}
      }
      log.info(`[Background] Synced ${success}/${data.length} wave stats to remote`)
    })
  }
  return { success: true, localCount: data.length }
}

export async function addDurationStats(data: { anchor_id: string; date: string; duration_minutes: number }[]) {
  if (!localDb) throw new Error('Local database not initialized')
  log.info(`Adding ${data.length} duration stats to local...`)

  for (const item of data) {
    localDb.run(`INSERT INTO duration_stats (anchor_id, date, duration_minutes) VALUES (?, ?, ?)
      ON CONFLICT(anchor_id, date) DO UPDATE SET duration_minutes = excluded.duration_minutes`,
      [item.anchor_id, item.date, item.duration_minutes])
  }
  saveDatabase()
  log.info(`Added ${data.length} duration stats to local`)

  // 后台同步到远程
  if (remotePool) {
    setImmediate(async () => {
      let success = 0
      for (const item of data) {
        try {
          await remotePool!.query(
            `INSERT INTO duration_stats (anchor_id, date, duration_minutes) VALUES (?, ?, ?)
             ON DUPLICATE KEY UPDATE duration_minutes = VALUES(duration_minutes)`,
            [item.anchor_id, item.date, item.duration_minutes])
          success++
        } catch (e) {}
      }
      log.info(`[Background] Synced ${success}/${data.length} duration stats to remote`)
    })
  }
  return { success: true, localCount: data.length }
}

export interface StatsFilters {
  anchor_id?: string
  startDate?: string
  endDate?: string
}

export async function getWaveStats(filters?: StatsFilters) {
  if (!localDb) throw new Error('Local database not initialized')

  let query = 'SELECT w.*, a.anchor_name FROM wave_stats w LEFT JOIN anchors a ON w.anchor_id = a.anchor_id WHERE 1=1'
  const params: any[] = []
  if (filters?.anchor_id) { query += ' AND w.anchor_id = ?'; params.push(filters.anchor_id) }
  if (filters?.startDate) { query += ' AND w.date >= ?'; params.push(filters.startDate) }
  if (filters?.endDate) { query += ' AND w.date <= ?'; params.push(filters.endDate) }
  query += ' ORDER BY w.date DESC, w.rank ASC'

  const result = localDb.exec(query, params)
  if (result.length === 0) return []
  const columns = result[0].columns
  return result[0].values.map((row: any[]) => {
    const obj: any = {}
    columns.forEach((col: string, i: number) => { obj[col] = row[i] })
    return obj
  })
}

export async function getDurationStats(filters?: StatsFilters) {
  if (!localDb) throw new Error('Local database not initialized')

  let query = 'SELECT d.*, a.anchor_name FROM duration_stats d LEFT JOIN anchors a ON d.anchor_id = a.anchor_id WHERE 1=1'
  const params: any[] = []
  if (filters?.anchor_id) { query += ' AND d.anchor_id = ?'; params.push(filters.anchor_id) }
  if (filters?.startDate) { query += ' AND d.date >= ?'; params.push(filters.startDate) }
  if (filters?.endDate) { query += ' AND d.date <= ?'; params.push(filters.endDate) }
  query += ' ORDER BY d.date DESC'

  const result = localDb.exec(query, params)
  if (result.length === 0) return []
  const columns = result[0].columns
  return result[0].values.map((row: any[]) => {
    const obj: any = {}
    columns.forEach((col: string, i: number) => { obj[col] = row[i] })
    return obj
  })
}

export type ClearDataType = 'local' | 'remote' | 'both'

export async function checkAuthorization(): Promise<{ authorized: boolean; error?: string }> {
  // 尝试创建一个新的连接来检查授权，而不依赖于已初始化的连接池
  try {
    const tempPool = mysql.createPool(remoteConfig);
    // 测试连接
    await tempPool.query('SELECT 1');
    
    try {
      // 从远程数据库的shouquan表查询第一个记录的shouquan字段
      const [rows] = await tempPool.query<any[]>('SELECT shouquan FROM shouquan LIMIT 1');
      if (rows.length > 0) {
        const authValue = rows[0].shouquan;
        // 检查授权值是否等于1
        if (authValue == 1) {
          await tempPool.end(); // 关闭临时连接
          return { authorized: true };
        } else {
          await tempPool.end(); // 关闭临时连接
          return { authorized: false, error: `授权验证失败: shouquan值为${authValue}，需要为1` };
        }
      } else {
        await tempPool.end(); // 关闭临时连接
        return { authorized: false, error: '未找到授权信息' };
      }
    } catch (shouquanError) {
      // 如果shouquan表不存在，也检查user表作为备选
      try {
        const [rows] = await tempPool.query<any[]>('SELECT shouquan FROM user LIMIT 1');
        if (rows.length > 0) {
          const authValue = rows[0].shouquan;
          await tempPool.end(); // 关闭临时连接
          if (authValue == 1) {
            return { authorized: true };
          } else {
            return { authorized: false, error: `授权验证失败: shouquan值为${authValue}，需要为1` };
          }
        } else {
          await tempPool.end(); // 关闭临时连接
          return { authorized: false, error: '未找到授权信息' };
        }
      } catch (userError) {
        await tempPool.end(); // 关闭临时连接
        return { authorized: false, error: `授权检查失败: ${String(shouquanError)}` };
      }
    }
  } catch (connectionError) {
    return { authorized: false, error: `远程数据库连接失败: ${String(connectionError)}` };
  }
}

export async function clearStatsData(type: ClearDataType) {
  const results = { local: { wave: 0, duration: 0 }, remote: { wave: 0, duration: 0 } }
  const warnings: string[] = []

  if (type === 'local' || type === 'both') {
    if (!localDb) throw new Error('Local database not initialized')
    localDb.run('DELETE FROM wave_stats')
    localDb.run('DELETE FROM duration_stats')
    saveDatabase()
    results.local.wave = 1
    results.local.duration = 1
    log.info('Cleared local stats data')
  }

  if (type === 'remote' || type === 'both') {
    if (remotePool) {
      try {
        const [waveRes] = await remotePool.query<any>('DELETE FROM wave_stats')
        const [durRes] = await remotePool.query<any>('DELETE FROM duration_stats')
        results.remote.wave = waveRes.affectedRows || 0
        results.remote.duration = durRes.affectedRows || 0
        log.info('Cleared remote stats data')
      } catch (e) {
        warnings.push('远程数据清除失败: ' + String(e))
      }
    }
  }
  return { success: true, results, warnings }
}
