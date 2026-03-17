import { app, BrowserWindow, ipcMain, dialog } from 'electron'
import path from 'path'
import fs from 'fs'
import log from 'electron-log'
import { initDatabases, syncAllData, getAnchors, addWaveStats, addDurationStats, getWaveStats, getDurationStats, addAnchor, updateAnchor, deleteAnchor, getNextSerialNumber, clearStatsData, checkAuthorization } from './database'

// Configure logging
log.transports.file.level = 'info'
log.transports.console.level = 'debug'

// Global exception handler
process.on('uncaughtException', (error) => {
  log.error('Uncaught Exception:', error)
  app.exit(1)
})

process.on('unhandledRejection', (reason) => {
  log.error('Unhandled Rejection:', reason)
})

let mainWindow: BrowserWindow | null = null

const VITE_DEV_SERVER_URL = process.env.VITE_DEV_SERVER_URL

function createWindow() {
  log.info('Creating main window...')

  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1200,
    minHeight: 700,
    backgroundColor: '#0F172A',
    frame: false,           // 无边框窗口
    autoHideMenuBar: true,  // 隐藏菜单栏
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true
    },
    show: false
  })

  mainWindow.once('ready-to-show', () => {
    mainWindow?.show()
    log.info('Window ready to show')
  })

  if (VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(VITE_DEV_SERVER_URL)
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }
}

// IPC Handlers
ipcMain.handle('dialog:openFile', async (_, filters) => {
  const result = await dialog.showOpenDialog(mainWindow!, {
    properties: ['openFile'],
    filters: filters || [
      { name: 'CSV Files', extensions: ['csv'] }
    ]
  })
  return result.filePaths
})

ipcMain.handle('fs:readFile', async (_, filePath: string) => {
  try {
    const content = fs.readFileSync(filePath, 'utf-8')
    return { success: true, data: content }
  } catch (error) {
    log.error('Read file error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('dialog:saveFile', async (_, options) => {
  const result = await dialog.showSaveDialog(mainWindow!, {
    defaultPath: options?.defaultPath,
    filters: options?.filters || [
      { name: 'CSV Files', extensions: ['csv'] }
    ]
  })
  return result
})

ipcMain.handle('fs:copyFile', async (_, source: string, dest: string) => {
  try {
    fs.copyFileSync(source, dest)
    return { success: true }
  } catch (error) {
    log.error('Copy file error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('fs:writeFile', async (_, filePath: string, content: string) => {
  try {
    fs.writeFileSync(filePath, content, 'utf-8')
    return { success: true }
  } catch (error) {
    log.error('Write file error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('path:dirname', async (_, filePath: string) => {
  return path.dirname(filePath)
})

ipcMain.handle('path:basename', async (_, filePath: string) => {
  return path.basename(filePath)
})

ipcMain.handle('path:extname', async (_, filePath: string) => {
  return path.extname(filePath)
})

ipcMain.handle('path:join', async (_, ...paths: string[]) => {
  return path.join(...paths)
})

ipcMain.handle('db:getNextSerialNumber', async () => {
  try {
    const serialNumber = await getNextSerialNumber()
    return { success: true, data: serialNumber }
  } catch (error) {
    log.error('Get next serial number error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('db:syncAllData', async () => {
  try {
    const result = await syncAllData()
    return { success: true, data: result }
  } catch (error) {
    log.error('Sync all data error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('db:getAnchors', async () => {
  try {
    const anchors = await getAnchors()
    return { success: true, data: anchors }
  } catch (error) {
    log.error('Get anchors error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('db:addWaveStats', async (_, data) => {
  try {
    log.info(`Received addWaveStats request with ${data?.length || 0} records`)
    if (!data || !Array.isArray(data) || data.length === 0) {
      return { success: false, error: '无效的数据' }
    }
    await addWaveStats(data)
    log.info('addWaveStats completed successfully')
    return { success: true }
  } catch (error) {
    log.error('Add wave stats error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('db:addDurationStats', async (_, data) => {
  try {
    log.info(`Received addDurationStats request with ${data?.length || 0} records`)
    if (!data || !Array.isArray(data) || data.length === 0) {
      return { success: false, error: '无效的数据' }
    }
    await addDurationStats(data)
    log.info('addDurationStats completed successfully')
    return { success: true }
  } catch (error) {
    log.error('Add duration stats error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('db:getWaveStats', async (_, filters) => {
  try {
    const stats = await getWaveStats(filters)
    return { success: true, data: stats }
  } catch (error) {
    log.error('Get wave stats error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('db:getDurationStats', async (_, filters) => {
  try {
    const stats = await getDurationStats(filters)
    return { success: true, data: stats }
  } catch (error) {
    log.error('Get duration stats error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('db:addAnchor', async (_, data) => {
  try {
    await addAnchor(data)
    return { success: true }
  } catch (error) {
    log.error('Add anchor error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('db:updateAnchor', async (_, id, data) => {
  try {
    await updateAnchor(id, data)
    return { success: true }
  } catch (error) {
    log.error('Update anchor error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('db:deleteAnchor', async (_, id) => {
  try {
    await deleteAnchor(id)
    return { success: true }
  } catch (error) {
    log.error('Delete anchor error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('db:clearStatsData', async (_, type) => {
  try {
    const result = await clearStatsData(type)
    return { success: true, results: result.results }
  } catch (error) {
    log.error('Clear stats data error:', error)
    return { success: false, error: String(error) }
  }
})

ipcMain.handle('db:checkAuthorization', async () => {
  try {
    const result = await checkAuthorization()
    return { ...result }
  } catch (error) {
    log.error('Authorization check error:', error)
    return { authorized: false, error: '系统错误' }
  }
})

// 窗口控制
ipcMain.handle('window:minimize', async () => {
  mainWindow?.minimize()
})

ipcMain.handle('window:maximize', async () => {
  if (mainWindow?.isMaximized()) {
    mainWindow.unmaximize()
  } else {
    mainWindow?.maximize()
  }
})

ipcMain.handle('window:close', async () => {
  mainWindow?.close()
})

ipcMain.handle('window:isMaximized', async () => {
  return mainWindow?.isMaximized() || false
})

app.whenReady().then(async () => {
  log.info('App ready, initializing databases...')

  try {
    await initDatabases()
    log.info('Databases initialized successfully')

    // 从远程同步所有数据到本地
    try {
      log.info('Starting data sync from remote...')
      await syncAllData()
      log.info('Data sync completed')
    } catch (syncError) {
      log.warn('Failed to sync data:', syncError)
    }
  } catch (error) {
    log.error('Failed to initialize databases:', error)
  }

  createWindow()

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})
