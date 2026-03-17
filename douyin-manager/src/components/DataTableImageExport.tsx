import { useState, useEffect, useRef, useMemo } from 'react'
import { WaveStat, DurationStat, Anchor } from '../types'

interface DataTableImageExportProps {
  anchors: Anchor[]
  date: string
  allWaveStats?: WaveStat[]
  allDurationStats?: DurationStat[]
  onClose: () => void
}

// 默认等级阈值配置（单位：万）
const DEFAULT_GRADE_THRESHOLDS = {
  S: 300,   // 300万以上
  A: 150,   // 150万以上
  B: 80,    // 80万以上
  C: 30     // 30万以上，以下为D
}

// 根据阈值配置获取等级字母
const getGradeLetterByThresholds = (waveValue: number, thresholds: typeof DEFAULT_GRADE_THRESHOLDS): string => {
  const wan = waveValue / 10000
  if (wan >= thresholds.S) return 'S'
  if (wan >= thresholds.A) return 'A'
  if (wan >= thresholds.B) return 'B'
  if (wan >= thresholds.C) return 'C'
  return 'D'
}

// 等级排序权重
const getGradeWeight = (grade: string): number => {
  const letter = grade.charAt(0)
  const num = grade.length > 1 ? parseInt(grade.slice(1)) : 999  // D等级排最后
  
  const letterWeight: Record<string, number> = {
    'S': 1,
    'A': 2,
    'B': 3,
    'C': 4,
    'D': 5
  }
  
  // S1 < S2 < S3 < ... < A1 < A2 < A3...
  return (letterWeight[letter] || 5) * 1000 + num
}

// 格式化音浪数值
const formatWave = (num: number): string => {
  if (num >= 10000) {
    const wan = Math.floor(num / 10000)
    const remainder = num % 10000
    if (remainder > 0) {
      return `${wan}万${remainder}`
    }
    return `${wan}万`
  }
  return num.toLocaleString()
}

// 格式化日期字符串
const formatDateStr = (date: Date | string): string => {
  if (typeof date === 'string') return date
  return date.toISOString().split('T')[0]
}

// 获取月份天数
const getDaysInMonth = (year: number, month: number): number => {
  return new Date(year, month, 0).getDate()
}

// 获取月份第一天是星期几
const getFirstDayOfMonth = (year: number, month: number): number => {
  return new Date(year, month - 1, 1).getDay()
}

function DataTableImageExport({ anchors, date, allWaveStats = [], allDurationStats = [], onClose }: DataTableImageExportProps) {
  // 找出最新有数据的日期作为默认值
  const getDefaultDate = useMemo(() => {
    if (allWaveStats.length === 0) return date
    // 按日期降序排序，取最新的
    const sortedDates = [...new Set(allWaveStats.map(item => formatDateStr(item.date)))].sort((a, b) => b.localeCompare(a))
    return sortedDates[0] || date
  }, [allWaveStats, date])

  const [exporting, setExporting] = useState(false)
  const [genderFilter, setGenderFilter] = useState<'all' | 'male' | 'female'>('all')
  const [selectedDate, setSelectedDate] = useState(getDefaultDate)
  const [currentMonth, setCurrentMonth] = useState(() => {
    const parts = getDefaultDate.split('-')
    return { year: parseInt(parts[0]), month: parseInt(parts[1]) }
  })
  const [showCalendar, setShowCalendar] = useState(false)
  
  // 等级阈值配置（单位：万）
  const [gradeThresholds, setGradeThresholds] = useState(DEFAULT_GRADE_THRESHOLDS)
  const [showGradeConfig, setShowGradeConfig] = useState(false)
  
  // 可选列
  const [showWave, setShowWave] = useState(true)
  const [showTotalWave, setShowTotalWave] = useState(true)
  const [showGradeCol, setShowGradeCol] = useState(true)
  const [showDuration, setShowDuration] = useState(true)
  
  // 排序方式
  const [sortBy, setSortBy] = useState<'wave' | 'totalWave' | 'grade' | 'gender'>('totalWave')
  
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const calendarRef = useRef<HTMLDivElement>(null)
  const gradeConfigRef = useRef<HTMLDivElement>(null)

  // 点击外部关闭日历和等级配置
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (calendarRef.current && !calendarRef.current.contains(e.target as Node)) {
        setShowCalendar(false)
      }
      if (gradeConfigRef.current && !gradeConfigRef.current.contains(e.target as Node)) {
        setShowGradeConfig(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  // 绘制圆角矩形的兼容性函数
  const drawRoundRect = (ctx: CanvasRenderingContext2D, x: number, y: number, width: number, height: number, radius: number) => {
    if (ctx.roundRect) {
      ctx.roundRect(x, y, width, height, radius)
    } else {
      ctx.beginPath()
      ctx.moveTo(x + radius, y)
      ctx.lineTo(x + width - radius, y)
      ctx.quadraticCurveTo(x + width, y, x + width, y + radius)
      ctx.lineTo(x + width, y + height - radius)
      ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height)
      ctx.lineTo(x + radius, y + height)
      ctx.quadraticCurveTo(x, y + height, x, y + height - radius)
      ctx.lineTo(x, y + radius)
      ctx.quadraticCurveTo(x, y, x + radius, y)
      ctx.closePath()
    }
  }

  // 获取主播性别
  const getAnchorGender = (anchorId: string): string => {
    const anchor = anchors.find(a => a.anchor_id === anchorId)
    return anchor?.gender === 'male' ? '男' : anchor?.gender === 'female' ? '女' : ''
  }

  // 获取所有有数据的日期列表
  const availableDates = useMemo(() => {
    const dateSet = new Set<string>()
    allWaveStats.forEach(item => {
      const d = formatDateStr(item.date)
      if (d) dateSet.add(d)
    })
    allDurationStats.forEach(item => {
      const d = formatDateStr(item.date)
      if (d) dateSet.add(d)
    })
    return dateSet
  }, [allWaveStats, allDurationStats])

  // 获取选择日期的音浪数据
  const selectedDateWaveStats = useMemo(() => {
    return allWaveStats.filter(item => formatDateStr(item.date) === selectedDate)
  }, [allWaveStats, selectedDate])

  // 获取选择日期的时长数据
  const selectedDateDurationStats = useMemo(() => {
    return allDurationStats.filter(item => formatDateStr(item.date) === selectedDate)
  }, [allDurationStats, selectedDate])

  // 计算本月的累计音浪
  const totalWaveMap = useMemo(() => {
    const map = new Map<string, number>()
    // 获取选择日期所在月份
    const [year, month] = selectedDate.split('-')
    const yearMonth = `${year}-${month}`
    
    allWaveStats.forEach(item => {
      const itemDate = formatDateStr(item.date)
      if (itemDate) {
        const [itemYear, itemMonth] = itemDate.split('-')
        const itemYearMonth = `${itemYear}-${itemMonth}`
        // 只累计同月的数据
        if (itemYearMonth === yearMonth) {
          map.set(item.anchor_id, (map.get(item.anchor_id) || 0) + item.wave_value)
        }
      }
    })
    return map
  }, [allWaveStats, selectedDate])

  // 获取选择日期的时长数据（如果没有，则查找该主播最近一次的时长数据）
  const durationMap = useMemo(() => {
    const map = new Map<string, number>()
    
    // 首先从选定日期获取
    selectedDateDurationStats.forEach(item => {
      map.set(item.anchor_id, item.duration_minutes)
    })
    
    // 对于选定日期没有数据的主播，查找最近一次的时长数据
    if (selectedDateDurationStats.length === 0) {
      // 如果选定日期完全没有数据，则从所有时长数据中按日期倒序查找每个主播的最新数据
      const latestByAnchor = new Map<string, { date: string; duration: number }>()
      allDurationStats.forEach(item => {
        const itemDate = formatDateStr(item.date)
        const existing = latestByAnchor.get(item.anchor_id)
        if (!existing || itemDate > existing.date) {
          latestByAnchor.set(item.anchor_id, { date: itemDate, duration: item.duration_minutes })
        }
      })
      latestByAnchor.forEach((value, anchorId) => {
        if (!map.has(anchorId)) {
          map.set(anchorId, value.duration)
        }
      })
    }
    
    return map
  }, [selectedDateDurationStats, allDurationStats])

  // 过滤主播（按性别）
  const filteredAnchors = useMemo(() => {
    return anchors.filter(anchor => {
      if (genderFilter === 'all') return true
      return genderFilter === 'male' ? anchor.gender === 'male' : anchor.gender === 'female'
    })
  }, [anchors, genderFilter])

  // 创建当日音浪数据映射
  const waveDataMap = useMemo(() => {
    const map = new Map<string, typeof selectedDateWaveStats[0]>()
    selectedDateWaveStats.forEach(item => {
      map.set(item.anchor_id, item)
    })
    return map
  }, [selectedDateWaveStats])

  // 合并数据 - 以主播列表为基础，没有音浪数据的显示"未开播"
  const mergedData = useMemo(() => {
    return filteredAnchors.map(anchor => {
      const waveData = waveDataMap.get(anchor.anchor_id)
      return {
        anchor_id: anchor.anchor_id,
        anchor_name: anchor.anchor_name || anchor.anchor_id,
        date: waveData?.date || selectedDate,
        wave_value: waveData?.wave_value || 0,  // 没有音浪数据时为0，显示"未开播"
        totalWave: totalWaveMap.get(anchor.anchor_id) || 0,
        duration: durationMap.get(anchor.anchor_id) || 0,
        gender: getAnchorGender(anchor.anchor_id),
        id: waveData?.id || 0
      }
    })
  }, [filteredAnchors, waveDataMap, totalWaveMap, durationMap, selectedDate])

  // 排序并分配等级（等级数字根据排名递增，如S1,S2,S3,S4...）
  const sortedData = useMemo(() => {
    // 先排序
    let sorted: typeof mergedData
    if (sortBy === 'wave') {
      sorted = [...mergedData].sort((a, b) => b.wave_value - a.wave_value)
    } else if (sortBy === 'totalWave') {
      sorted = [...mergedData].sort((a, b) => b.totalWave - a.totalWave)
    } else if (sortBy === 'gender') {
      sorted = [...mergedData].sort((a, b) => {
        // 男在前，女在后
        if (a.gender === '男' && b.gender !== '男') return -1
        if (a.gender !== '男' && b.gender === '男') return 1
        // 同性别按音浪排序
        return b.wave_value - a.wave_value
      })
    } else {
      // 按等级排序时，先按音浪排序计算等级
      sorted = [...mergedData].sort((a, b) => b.wave_value - a.wave_value)
    }
    
    // 统计每个等级字母的人数，用于分配数字
    const gradeCountMap = new Map<string, number>()
    
    // 等级始终按照总音浪进行定级
    // 分配等级（数字根据排名递增：S1,S2,S3,S4...D1,D2,D3...）
    const dataWithGrade = sorted.map(item => {
      const gradeValue = item.totalWave  // 始终使用总音浪定级
      const letter = getGradeLetterByThresholds(gradeValue, gradeThresholds)
      const count = gradeCountMap.get(letter) || 0
      gradeCountMap.set(letter, count + 1)
      
      // 数字根据排名递增：第1个是1，第2个是2，第3个是3...
      const num = count + 1
      const grade = `${letter}${num}`
      
      return {
        ...item,
        grade
      }
    })
    
    // 如果按等级排序，重新排序
    if (sortBy === 'grade') {
      return dataWithGrade.sort((a, b) => getGradeWeight(a.grade) - getGradeWeight(b.grade))
    }
    
    return dataWithGrade
  }, [mergedData, sortBy, gradeThresholds])

  // 快捷日期选择
  const handleQuickDate = (type: 'today' | 'yesterday') => {
    const now = new Date()
    if (type === 'yesterday') {
      now.setDate(now.getDate() - 1)
    }
    const dateStr = now.toISOString().split('T')[0]
    setSelectedDate(dateStr)
    setCurrentMonth({ year: now.getFullYear(), month: now.getMonth() + 1 })
  }

  // 获取今天和昨天的日期
  const today = new Date().toISOString().split('T')[0]
  const yesterday = new Date(Date.now() - 86400000).toISOString().split('T')[0]

  // 绘制Canvas
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const scale = 2
    
    // 构建标题
    const genderText = genderFilter === 'male' ? '男' : genderFilter === 'female' ? '女' : ''
    const parts = selectedDate.split('-')
    const year = parseInt(parts[0]) || 2026
    const month = parseInt(parts[1]) || 1
    const day = parseInt(parts[2]) || 1
    const titleText = `星嗨艺创${genderText}主播 ${year}年${month}月${day}日音浪表`

    // 计算标题所需宽度
    ctx.font = `bold ${22 * scale}px sans-serif`
    const titleWidth = ctx.measureText(titleText).width
    const containerWidth = Math.max(600 * scale, Math.min(750 * scale, titleWidth + 60 * scale))

    // 构建列配置（序号和姓名为必选）
    const columns: { key: string; label: string; width: number }[] = []
    columns.push({ key: 'rank', label: '序号', width: 0.06 })
    columns.push({ key: 'name', label: '主播姓名', width: 0.12 })
    if (showWave) columns.push({ key: 'wave', label: `${day}号音浪`, width: 0.22 })
    if (showTotalWave) columns.push({ key: 'totalWave', label: '累计总音浪', width: 0.22 })
    if (showGradeCol) columns.push({ key: 'grade', label: '等级', width: 0.10 })
    if (showDuration) columns.push({ key: 'duration', label: '有效时长', width: 0.14 })

    // 重新计算列宽比例
    const totalWidth = columns.reduce((sum, col) => sum + col.width, 0)
    columns.forEach(col => col.width = col.width / totalWidth)

    const headerHeight = 50 * scale
    const tableHeaderHeight = 28 * scale
    const rowHeight = 36 * scale

    // 未开播统计
    const inactiveStreamers = sortedData.filter(d => d.wave_value <= 1)
    const hasInactive = inactiveStreamers.length > 0
    const footerHeight = hasInactive ? 100 * scale : 0

    // 计算高度
    const totalHeight = headerHeight + tableHeaderHeight + (sortedData.length * rowHeight) + footerHeight

    canvas.width = containerWidth
    canvas.height = totalHeight

    // 颜色定义
    const headerDark = '#1E293B'
    const headerBg = '#f1f5f9'
    const headerText = '#64748b'
    const textMain = '#334155'
    const textGray = '#64748b'
    const dataBarColor = '#93c5fd'
    const absentBg = '#fff1f2'
    const absentRed = '#e11d48'

    // 背景色
    ctx.fillStyle = '#F8FAFC'
    ctx.fillRect(0, 0, containerWidth, totalHeight)

    let currentY = 0

    // 标题栏
    ctx.fillStyle = headerDark
    ctx.fillRect(0, currentY, containerWidth, headerHeight)

    ctx.fillStyle = '#f8fafc'
    ctx.font = `bold ${22 * scale}px sans-serif`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(titleText, containerWidth / 2, currentY + headerHeight / 2)
    currentY += headerHeight

    // 表头
    ctx.fillStyle = headerBg
    ctx.fillRect(0, currentY, containerWidth, tableHeaderHeight)

    ctx.fillStyle = headerText
    ctx.font = `bold ${13 * scale}px sans-serif`
    ctx.textBaseline = 'middle'

    columns.forEach((col, index) => {
      const x = columns.slice(0, index).reduce((sum, c) => sum + c.width * containerWidth, 0)
      const width = col.width * containerWidth
      const centerX = x + width / 2

      if (col.key === 'name') {
        ctx.textAlign = 'left'
        ctx.fillText(col.label.toUpperCase(), x + width * 0.1, currentY + tableHeaderHeight / 2)
      } else if (col.key === 'wave' || col.key === 'totalWave') {
        ctx.textAlign = 'right'
        ctx.fillText(col.label.toUpperCase(), x + width - width * 0.08, currentY + tableHeaderHeight / 2)
      } else {
        ctx.textAlign = 'center'
        ctx.fillText(col.label.toUpperCase(), centerX, currentY + tableHeaderHeight / 2)
      }
    })
    currentY += tableHeaderHeight

    // 计算最大音浪值
    const maxWave = Math.max(...sortedData.filter(d => d.wave_value > 1).map(d => d.wave_value), 1)

    // 数据行
    sortedData.forEach((item, index) => {
      const actualRank = index + 1
      const isTop3 = actualRank <= 3
      const isInactive = item.wave_value <= 1

      // 行背景
      if (isTop3 && actualRank === 1) {
        ctx.fillStyle = '#fffbeb'
      } else if (isTop3 && actualRank === 2) {
        ctx.fillStyle = '#f8fafc'
      } else if (isTop3 && actualRank === 3) {
        ctx.fillStyle = '#fafafa'
      } else {
        const gradeLetter = item.grade.charAt(0)
        if (['S', 'A', 'B'].includes(gradeLetter)) {
          ctx.fillStyle = '#fffef5'
        } else if (gradeLetter === 'C') {
          ctx.fillStyle = '#f0fdf4'
        } else if (gradeLetter === 'D') {
          ctx.fillStyle = '#f0f9ff'
        } else {
          ctx.fillStyle = '#FFFFFF'
        }
      }
      ctx.fillRect(0, currentY, containerWidth, rowHeight)

      // 分割线
      ctx.strokeStyle = '#f0f0f0'
      ctx.lineWidth = 0.5 * scale
      ctx.beginPath()
      ctx.moveTo(0, currentY)
      ctx.lineTo(containerWidth, currentY)
      ctx.stroke()

      // 绘制各列
      let colX = 0
      columns.forEach((col) => {
        const colWidth = col.width * containerWidth
        const centerX = colX + colWidth / 2
        const centerY = currentY + rowHeight / 2

        if (col.key === 'rank') {
          if (isTop3) {
            const icon = actualRank === 1 ? '🥇⭐' : actualRank === 2 ? '🥈' : '🥉'
            ctx.font = `${20 * scale}px sans-serif`
            ctx.fillStyle = textMain
            ctx.textAlign = 'center'
            ctx.fillText(icon, centerX, centerY)
          } else {
            ctx.font = `italic bold ${15 * scale}px serif`
            ctx.fillStyle = textGray
            ctx.textAlign = 'center'
            ctx.fillText(actualRank.toString().padStart(2, '0'), centerX, centerY)
          }
        } else if (col.key === 'name') {
          ctx.font = `${15 * scale}px sans-serif`
          ctx.fillStyle = textMain
          ctx.textAlign = 'left'
          const displayName = item.anchor_name.length > 6 ? item.anchor_name.slice(0, 6) + '...' : item.anchor_name
          ctx.fillText(displayName, colX + colWidth * 0.1, centerY)
        } else if (col.key === 'wave') {
          if (!isInactive) {
            const barWidthRatio = item.wave_value / maxWave
            const barMaxWidth = colWidth * 0.85
            const barWidth = barMaxWidth * barWidthRatio
            const barHeight = 20 * scale
            const barRight = colX + colWidth - colWidth * 0.05
            const barLeft = barRight - barWidth
            const barTop = currentY + (rowHeight - barHeight) / 2

            ctx.fillStyle = dataBarColor
            ctx.beginPath()
            drawRoundRect(ctx, barLeft, barTop, barWidth, barHeight, 4 * scale)
            ctx.fill()
          }

          if (isInactive) {
            ctx.font = `bold ${14 * scale}px sans-serif`
            ctx.fillStyle = absentRed
          } else {
            ctx.font = `${15 * scale}px monospace`
            ctx.fillStyle = textMain
          }
          ctx.textAlign = 'right'
          ctx.fillText(isInactive ? '未开播' : formatWave(item.wave_value), colX + colWidth - colWidth * 0.08, centerY)
        } else if (col.key === 'totalWave') {
          ctx.font = `${13 * scale}px monospace`
          ctx.fillStyle = textGray
          ctx.textAlign = 'right'
          ctx.fillText(formatWave(item.totalWave), colX + colWidth - colWidth * 0.08, centerY)
        } else if (col.key === 'grade') {
          const gradeLetter = item.grade.charAt(0)

          let bgColor: string, textColor: string
          if (gradeLetter === 'S') {
            bgColor = '#fef3c7'
            textColor = '#b45309'
          } else if (gradeLetter === 'A') {
            bgColor = '#fed7aa'
            textColor = '#c2410c'
          } else if (gradeLetter === 'B') {
            bgColor = '#fecaca'
            textColor = '#b91c1c'
          } else if (gradeLetter === 'C') {
            bgColor = '#a7f3d0'
            textColor = '#047857'
          } else {
            bgColor = '#e0f2fe'
            textColor = '#0369a1'
          }

          const lvlWidth = 45 * scale
          const lvlHeight = 18 * scale
          const lvlLeft = centerX - lvlWidth / 2
          const lvlTop = centerY - lvlHeight / 2

          ctx.fillStyle = bgColor
          ctx.beginPath()
          drawRoundRect(ctx, lvlLeft, lvlTop, lvlWidth, lvlHeight, 6 * scale)
          ctx.fill()

          ctx.fillStyle = textColor
          ctx.font = `bold ${11 * scale}px sans-serif`
          ctx.textAlign = 'center'
          ctx.fillText(item.grade, centerX, centerY + 4 * scale)
        } else if (col.key === 'duration') {
          const durationMinutes = item.duration || 0
          let durationText: string
          if (durationMinutes >= 60) {
            const hours = Math.floor(durationMinutes / 60)
            const mins = durationMinutes % 60
            durationText = mins > 0 ? `${hours}时${mins}分` : `${hours}时`
          } else {
            durationText = `${durationMinutes}分`
          }
          
          ctx.font = `${14 * scale}px sans-serif`
          ctx.fillStyle = '#7c3aed'
          ctx.textAlign = 'center'
          ctx.fillText(durationText, centerX, centerY)
        }

        colX += colWidth
      })

      currentY += rowHeight
    })

    // 底部统计
    if (hasInactive) {
      ctx.fillStyle = absentBg
      ctx.fillRect(0, currentY, containerWidth, footerHeight)

      ctx.strokeStyle = '#fecaca'
      ctx.lineWidth = 0.5 * scale
      ctx.beginPath()
      ctx.moveTo(0, currentY)
      ctx.lineTo(containerWidth, currentY)
      ctx.stroke()

      const totalStreamers = sortedData.length
      const inactiveCount = inactiveStreamers.length

      const statGap = 100 * scale
      const stat1X = containerWidth / 2 - statGap
      const stat2X = containerWidth / 2 + statGap
      const statY = currentY + 25 * scale

      ctx.fillStyle = absentRed
      ctx.font = `bold ${24 * scale}px sans-serif`
      ctx.textAlign = 'center'
      ctx.fillText(totalStreamers.toString(), stat1X, statY)

      ctx.font = `${10 * scale}px sans-serif`
      ctx.fillText(`${genderText}主播人数`, stat1X, statY + 16 * scale)

      ctx.font = `bold ${24 * scale}px sans-serif`
      ctx.fillText(inactiveCount.toString(), stat2X, statY)

      ctx.font = `${10 * scale}px sans-serif`
      ctx.fillText('未开播人数', stat2X, statY + 16 * scale)

      // 处理未开播人员名字的多行显示
      const names = inactiveStreamers.map(d => d.anchor_name || d.anchor_id)
      const namesY = currentY + 50 * scale
      const maxLineWidth = containerWidth * 0.8 // 最大行宽为容器的80%
      const lineHeight = 28 * scale
      const fontSize = 16 * scale
      const padding = 10 * scale

      // 分割名字到多行
      const lines: string[] = []
      let currentLine = ''
      ctx.font = `bold ${fontSize}px sans-serif`

      for (const name of names) {
        const testLine = currentLine ? currentLine + '、' + name : name
        const metrics = ctx.measureText(testLine)
        
        if (metrics.width > maxLineWidth && currentLine !== '') {
          lines.push(currentLine)
          currentLine = name
        } else {
          currentLine = testLine
        }
      }
      if (currentLine) {
        lines.push(currentLine)
      }

      // 计算多行文字的总高度并居中放置
      const totalTextHeight = lines.length * lineHeight
      const startY = namesY + (footerHeight - 50 * scale - totalTextHeight) / 2

      // 绘制每一行
      lines.forEach((line, index) => {
        const y = startY + index * lineHeight
        
        // 计算背景框的宽度
        const textMetrics = ctx.measureText(line)
        const bgWidth = Math.min(textMetrics.width + padding * 2, containerWidth * 0.9)
        const bgLeft = (containerWidth - bgWidth) / 2
        const bgTop = y - lineHeight / 2 + padding / 2
        const bgHeight = lineHeight - padding

        // 绘制背景
        ctx.fillStyle = '#FFFFFF'
        ctx.beginPath()
        drawRoundRect(ctx, bgLeft, bgTop, bgWidth, bgHeight, 30 * scale)
        ctx.fill()

        // 绘制文字
        ctx.fillStyle = absentRed
        ctx.font = `bold ${fontSize}px sans-serif`
        ctx.textAlign = 'center'
        ctx.fillText(line, containerWidth / 2, y + lineHeight / 2 - padding / 2)
      })
    }
  }, [sortedData, genderFilter, showWave, showTotalWave, showGradeCol, showDuration, selectedDate, anchors, allWaveStats, allDurationStats, gradeThresholds])

  const handleExport = async () => {
    const canvas = canvasRef.current
    if (!canvas) return

    setExporting(true)
    try {
      const link = document.createElement('a')
      const genderText = genderFilter === 'male' ? '男' : genderFilter === 'female' ? '女' : '全部'
      link.download = `${selectedDate}_${genderText}_${sortedData.length}人.png`
      link.href = canvas.toDataURL('image/png')
      link.click()
      onClose()
    } catch (err) {
      console.error('Export error:', err)
      alert('导出失败: ' + String(err))
    } finally {
      setExporting(false)
    }
  }

  const handleExportCSV = async () => {
    if (sortedData.length === 0) {
      alert('没有可导出的数据')
      return
    }

    setExporting(true)
    try {
      const headers = ['排名', '主播ID', '主播姓名', `${day}号音浪`, '累计总音浪', '等级', '有效时长(分钟)', '性别', '日期']
      
      const rows = sortedData.map((item, index) => [
        index + 1,
        item.anchor_id,
        item.anchor_name,
        item.wave_value,
        item.totalWave,
        item.grade,
        item.duration,
        item.gender,
        selectedDate
      ])

      const csvContent = [headers, ...rows]
        .map(row => row.map(cell => `"${cell}"`).join(','))
        .join('\n')

      const genderText = genderFilter === 'male' ? '男' : genderFilter === 'female' ? '女' : '全部'
      const defaultFileName = `${selectedDate}_${genderText}_音浪数据_${sortedData.length}人.csv`
      const saveResult = await window.electronAPI.openSaveDialog({
        defaultPath: defaultFileName,
        filters: [{ name: 'CSV Files', extensions: ['csv'] }]
      })

      if (saveResult.canceled || !saveResult.filePath) {
        return
      }

      const writeResult = await window.electronAPI.writeFile(saveResult.filePath, '\ufeff' + csvContent)
      if (!writeResult.success) {
        throw new Error(writeResult.error || '写入文件失败')
      }

      alert('导出成功！')
      onClose()
    } catch (err) {
      console.error('Export CSV error:', err)
      alert('导出失败: ' + String(err))
    } finally {
      setExporting(false)
    }
  }

  const parts = selectedDate.split('-')
  const day = parseInt(parts[2]) || 1

  // 生成日历数据
  const calendarDays = useMemo(() => {
    const days: (number | null)[] = []
    const firstDay = getFirstDayOfMonth(currentMonth.year, currentMonth.month)
    const daysInMonth = getDaysInMonth(currentMonth.year, currentMonth.month)
    
    // 填充前面的空白
    for (let i = 0; i < firstDay; i++) {
      days.push(null)
    }
    // 填充日期
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i)
    }
    return days
  }, [currentMonth])

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto py-4">
      <div className="bg-background-light rounded-xl w-[850px] max-h-[95vh] border border-slate-700 flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700 shrink-0">
          <h3 className="text-lg font-semibold text-text">导出图片</h3>
          <button
            onClick={onClose}
            className="text-text-muted hover:text-text transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="p-4 overflow-auto flex-1">
          {/* 设置选项 */}
          <div className="mb-4 p-4 bg-slate-800 rounded-lg">
            <div className="flex flex-wrap gap-4">
              {/* 日期选择 */}
              <div className="relative" ref={calendarRef}>
                <label className="text-text-muted text-sm block mb-2">选择日期</label>
                <div className="flex gap-2 items-center">
                  {/* 快捷按钮 */}
                  <button
                    onClick={() => handleQuickDate('today')}
                    className={`px-3 py-1 rounded text-sm ${
                      selectedDate === today 
                        ? 'bg-accent-gold text-black' 
                        : 'bg-slate-700 text-text hover:bg-slate-600'
                    }`}
                  >
                    今日
                  </button>
                  <button
                    onClick={() => handleQuickDate('yesterday')}
                    className={`px-3 py-1 rounded text-sm ${
                      selectedDate === yesterday 
                        ? 'bg-accent-gold text-black' 
                        : 'bg-slate-700 text-text hover:bg-slate-600'
                    }`}
                  >
                    前一天
                  </button>
                  {/* 日期点选按钮 */}
                  <button
                    onClick={() => setShowCalendar(!showCalendar)}
                    className="px-3 py-1 rounded text-sm bg-slate-700 text-text hover:bg-slate-600 flex items-center gap-1"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                    </svg>
                    {selectedDate}
                  </button>
                </div>
                
                {/* 日历弹窗 */}
                {showCalendar && (
                  <div className="absolute top-full left-0 mt-2 bg-slate-700 rounded-lg shadow-xl z-10 p-3 w-72">
                    {/* 月份切换 */}
                    <div className="flex items-center justify-between mb-2">
                      <button
                        onClick={() => {
                          if (currentMonth.month === 1) {
                            setCurrentMonth({ year: currentMonth.year - 1, month: 12 })
                          } else {
                            setCurrentMonth({ ...currentMonth, month: currentMonth.month - 1 })
                          }
                        }}
                        className="p-1 hover:bg-slate-600 rounded"
                      >
                        <svg className="w-4 h-4 text-text" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                        </svg>
                      </button>
                      <span className="text-text font-medium">{currentMonth.year}年{currentMonth.month}月</span>
                      <button
                        onClick={() => {
                          if (currentMonth.month === 12) {
                            setCurrentMonth({ year: currentMonth.year + 1, month: 1 })
                          } else {
                            setCurrentMonth({ ...currentMonth, month: currentMonth.month + 1 })
                          }
                        }}
                        className="p-1 hover:bg-slate-600 rounded"
                      >
                        <svg className="w-4 h-4 text-text" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                        </svg>
                      </button>
                    </div>
                    
                    {/* 星期头 */}
                    <div className="grid grid-cols-7 gap-1 mb-1">
                      {['日', '一', '二', '三', '四', '五', '六'].map(d => (
                        <div key={d} className="text-center text-xs text-text-muted py-1">{d}</div>
                      ))}
                    </div>
                    
                    {/* 日期网格 */}
                    <div className="grid grid-cols-7 gap-1">
                      {calendarDays.map((d, i) => (
                        <div key={i} className="text-center">
                          {d && (
                            <button
                              onClick={() => {
                                const dateStr = `${currentMonth.year}-${String(currentMonth.month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
                                setSelectedDate(dateStr)
                                setShowCalendar(false)
                              }}
                              className={`w-8 h-8 rounded text-sm ${
                                selectedDate === `${currentMonth.year}-${String(currentMonth.month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
                                  ? 'bg-accent-gold text-black'
                                  : availableDates.has(`${currentMonth.year}-${String(currentMonth.month).padStart(2, '0')}-${String(d).padStart(2, '0')}`)
                                    ? 'hover:bg-slate-600 text-text bg-slate-700'
                                    : 'hover:bg-slate-600 text-text-muted'
                              }`}
                            >
                              {d}
                            </button>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* 性别筛选 */}
              <div>
                <label className="text-text-muted text-sm block mb-2">性别筛选</label>
                <div className="flex gap-2">
                  <button
                    onClick={() => setGenderFilter('all')}
                    className={`px-3 py-1 rounded text-sm ${genderFilter === 'all' ? 'bg-accent-gold text-black' : 'bg-slate-700 text-text'}`}
                  >
                    全部
                  </button>
                  <button
                    onClick={() => setGenderFilter('male')}
                    className={`px-3 py-1 rounded text-sm ${genderFilter === 'male' ? 'bg-accent-gold text-black' : 'bg-slate-700 text-text'}`}
                  >
                    男
                  </button>
                  <button
                    onClick={() => setGenderFilter('female')}
                    className={`px-3 py-1 rounded text-sm ${genderFilter === 'female' ? 'bg-accent-gold text-black' : 'bg-slate-700 text-text'}`}
                  >
                    女
                  </button>
                </div>
              </div>

              {/* 排序方式 */}
              <div>
                <label className="text-text-muted text-sm block mb-2">排序方式</label>
                <div className="flex gap-2 flex-wrap">
                  <button
                    onClick={() => setSortBy('wave')}
                    className={`px-3 py-1 rounded text-sm ${sortBy === 'wave' ? 'bg-accent-gold text-black' : 'bg-slate-700 text-text hover:bg-slate-600'}`}
                  >
                    日音浪
                  </button>
                  <button
                    onClick={() => setSortBy('totalWave')}
                    className={`px-3 py-1 rounded text-sm ${sortBy === 'totalWave' ? 'bg-accent-gold text-black' : 'bg-slate-700 text-text hover:bg-slate-600'}`}
                  >
                    总音浪
                  </button>
                  <button
                    onClick={() => setSortBy('grade')}
                    className={`px-3 py-1 rounded text-sm ${sortBy === 'grade' ? 'bg-accent-gold text-black' : 'bg-slate-700 text-text hover:bg-slate-600'}`}
                  >
                    等级
                  </button>
                  <button
                    onClick={() => setSortBy('gender')}
                    className={`px-3 py-1 rounded text-sm ${sortBy === 'gender' ? 'bg-accent-gold text-black' : 'bg-slate-700 text-text hover:bg-slate-600'}`}
                  >
                    性别
                  </button>
                </div>
              </div>

              {/* 等级阈值配置 */}
              <div className="relative" ref={gradeConfigRef}>
                <label className="text-text-muted text-sm block mb-2">等级阈值（万音浪）</label>
                <button
                  onClick={() => setShowGradeConfig(!showGradeConfig)}
                  className="px-3 py-1 rounded text-sm bg-slate-700 text-text hover:bg-slate-600 flex items-center gap-1"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  S≥{gradeThresholds.S} A≥{gradeThresholds.A} B≥{gradeThresholds.B} C≥{gradeThresholds.C}
                </button>
                
                {/* 配置弹窗 */}
                {showGradeConfig && (
                  <div className="absolute top-full left-0 mt-2 bg-slate-700 rounded-lg shadow-xl z-10 p-4 w-64">
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-text text-sm">S级 ≥</span>
                        <input
                          type="number"
                          value={gradeThresholds.S}
                          onChange={(e) => setGradeThresholds({ ...gradeThresholds, S: Number(e.target.value) })}
                          className="w-20 px-2 py-1 bg-slate-600 text-text rounded text-sm text-right"
                        />
                        <span className="text-text-muted text-sm">万</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-text text-sm">A级 ≥</span>
                        <input
                          type="number"
                          value={gradeThresholds.A}
                          onChange={(e) => setGradeThresholds({ ...gradeThresholds, A: Number(e.target.value) })}
                          className="w-20 px-2 py-1 bg-slate-600 text-text rounded text-sm text-right"
                        />
                        <span className="text-text-muted text-sm">万</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-text text-sm">B级 ≥</span>
                        <input
                          type="number"
                          value={gradeThresholds.B}
                          onChange={(e) => setGradeThresholds({ ...gradeThresholds, B: Number(e.target.value) })}
                          className="w-20 px-2 py-1 bg-slate-600 text-text rounded text-sm text-right"
                        />
                        <span className="text-text-muted text-sm">万</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-text text-sm">C级 ≥</span>
                        <input
                          type="number"
                          value={gradeThresholds.C}
                          onChange={(e) => setGradeThresholds({ ...gradeThresholds, C: Number(e.target.value) })}
                          className="w-20 px-2 py-1 bg-slate-600 text-text rounded text-sm text-right"
                        />
                        <span className="text-text-muted text-sm">万</span>
                      </div>
                      <div className="pt-2 border-t border-slate-600">
                        <button
                          onClick={() => setGradeThresholds(DEFAULT_GRADE_THRESHOLDS)}
                          className="w-full px-3 py-1 bg-slate-600 hover:bg-slate-500 text-text rounded text-sm"
                        >
                          恢复默认
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* 可选列 */}
              <div>
                <label className="text-text-muted text-sm block mb-2">可选列</label>
                <div className="flex gap-2 flex-wrap">
                  <label className="flex items-center gap-1 text-sm text-text">
                    <input type="checkbox" checked={showWave} onChange={(e) => setShowWave(e.target.checked)} className="accent-accent-gold" />
                    {day}号音浪
                  </label>
                  <label className="flex items-center gap-1 text-sm text-text">
                    <input type="checkbox" checked={showTotalWave} onChange={(e) => setShowTotalWave(e.target.checked)} className="accent-accent-gold" />
                    累计总音浪
                  </label>
                  <label className="flex items-center gap-1 text-sm text-text">
                    <input type="checkbox" checked={showGradeCol} onChange={(e) => setShowGradeCol(e.target.checked)} className="accent-accent-gold" />
                    等级
                  </label>
                  <label className="flex items-center gap-1 text-sm text-text">
                    <input type="checkbox" checked={showDuration} onChange={(e) => setShowDuration(e.target.checked)} className="accent-accent-gold" />
                    有效时长
                  </label>
                </div>
              </div>
            </div>
          </div>

          {/* 预览 */}
          <div className="flex justify-center overflow-auto">
            <canvas 
              ref={canvasRef} 
              style={{ maxWidth: '100%', height: 'auto', border: '1px solid #334155', borderRadius: '4px' }}
            />
          </div>
        </div>

        <div className="px-6 py-4 border-t border-slate-700 flex gap-3 shrink-0">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2 border border-slate-600 rounded-lg text-text-muted hover:bg-slate-700 transition-colors"
          >
            取消
          </button>
          <button
            onClick={handleExportCSV}
            disabled={exporting || sortedData.length === 0}
            className="flex-1 px-4 py-2 bg-accent-green/20 hover:bg-accent-green/30 disabled:opacity-50 rounded-lg text-accent-green font-medium transition-colors flex items-center justify-center gap-2"
          >
            {exporting ? (
              <>
                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                导出中...
              </>
            ) : (
              <>
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
                导出 CSV
              </>
            )}
          </button>
          <button
            onClick={handleExport}
            disabled={exporting || sortedData.length === 0}
            className="flex-1 px-4 py-2 bg-accent-gold hover:bg-yellow-500 disabled:opacity-50 rounded-lg text-black font-medium transition-colors flex items-center justify-center gap-2"
          >
            {exporting ? (
              <>
                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                导出中...
              </>
            ) : (
              <>
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                导出图片
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}

export default DataTableImageExport
