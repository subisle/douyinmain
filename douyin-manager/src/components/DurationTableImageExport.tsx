import { useState, useEffect, useRef, useMemo } from 'react'
import { DurationStat, Anchor } from '../types'

interface DurationTableImageExportProps {
  anchors: Anchor[]
  date: string
  allDurationStats?: DurationStat[]
  onClose: () => void
}

// 时长范围配置
interface DurationRange {
  id: string
  minHours: number
  reward: string
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

// 格式化时长
const formatDuration = (minutes: number): string => {
  if (minutes <= 0) return '0时'
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  if (mins > 0) {
    return `${hours}时${mins}分`
  }
  return `${hours}时`
}

// 默认时长范围 - 初始为空数组，导出时不配置默认奖励
const defaultDurationRanges: DurationRange[] = []

// 根据时长获取对应的范围
const getDurationRange = (minutes: number, ranges: DurationRange[]): DurationRange | null => {
  if (minutes <= 0) return null
  const hours = minutes / 60
  // 按最小时长降序排序，找到第一个满足条件的范围
  const sortedRanges = [...ranges].sort((a, b) => b.minHours - a.minHours)
  return sortedRanges.find(r => hours >= r.minHours) || null
}

function DurationTableImageExport({ anchors, date, allDurationStats = [], onClose }: DurationTableImageExportProps) {
  // 找出最新有数据的日期作为默认值
  const getDefaultDate = useMemo(() => {
    if (allDurationStats.length === 0) return date
    const sortedDates = [...new Set(allDurationStats.map(item => formatDateStr(item.date)))].sort((a, b) => b.localeCompare(a))
    return sortedDates[0] || date
  }, [allDurationStats, date])

  const [exporting, setExporting] = useState(false)
  const [genderFilter, setGenderFilter] = useState<'all' | 'male' | 'female'>('all')
  const [selectedDate, setSelectedDate] = useState(getDefaultDate)
  const [currentMonth, setCurrentMonth] = useState(() => {
    const parts = getDefaultDate.split('-')
    return { year: parseInt(parts[0]), month: parseInt(parts[1]) }
  })
  const [showCalendar, setShowCalendar] = useState(false)
  const [showRank, setShowRank] = useState(true)
  const [showName, setShowName] = useState(true)
  const [showDuration, setShowDuration] = useState(true)
  const [showRange, setShowRange] = useState(true)
  const [durationRanges, setDurationRanges] = useState<DurationRange[]>(defaultDurationRanges)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const calendarRef = useRef<HTMLDivElement>(null)

  // 点击外部关闭日历
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (calendarRef.current && !calendarRef.current.contains(e.target as Node)) {
        setShowCalendar(false)
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
    allDurationStats.forEach(item => {
      const d = formatDateStr(item.date)
      if (d) dateSet.add(d)
    })
    if (dateSet.size === 0) {
      dateSet.add(date)
    }
    return dateSet
  }, [allDurationStats, date])

  // 获取选择日期的时长数据
  const selectedDateDurationStats = useMemo(() => {
    return allDurationStats.filter(item => formatDateStr(item.date) === selectedDate)
  }, [allDurationStats, selectedDate])

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

  // 生成日历数据
  const calendarDays = useMemo(() => {
    const days: (number | null)[] = []
    const firstDay = getFirstDayOfMonth(currentMonth.year, currentMonth.month)
    const daysInMonth = getDaysInMonth(currentMonth.year, currentMonth.month)
    
    for (let i = 0; i < firstDay; i++) {
      days.push(null)
    }
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i)
    }
    return days
  }, [currentMonth])

  // 过滤数据
  const filteredData = selectedDateDurationStats.filter(item => {
    if (genderFilter === 'all') return true
    const gender = getAnchorGender(item.anchor_id)
    return genderFilter === 'male' ? gender === '男' : gender === '女'
  })

  // 排序
  const sortedData = [...filteredData].sort((a, b) => b.duration_minutes - a.duration_minutes)

  // 添加时长范围
  const addDurationRange = () => {
    const newId = Date.now().toString()
    setDurationRanges([...durationRanges, { id: newId, minHours: 0, reward: '' }])
  }

  // 删除时长范围
  const removeDurationRange = (id: string) => {
    setDurationRanges(durationRanges.filter(r => r.id !== id))
  }

  // 更新时长范围
  const updateDurationRange = (id: string, field: 'minHours' | 'reward', value: string | number) => {
    setDurationRanges(durationRanges.map(r => 
      r.id === id ? { ...r, [field]: field === 'minHours' ? Number(value) : value } : r
    ))
  }

  // 绘制Canvas
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const scale = 2
    const padding = 20 * scale
    const headerHeight = 60 * scale
    const tableHeaderHeight = 40 * scale
    const rowHeight = 44 * scale
    const footerHeight = sortedData.some(d => d.duration_minutes <= 0) ? 120 * scale : 0
    const rangeLegendHeight = durationRanges.filter(r => r.minHours > 0 && r.reward).length > 0 ? 50 * scale : 0

    // 构建标题
    const genderText = genderFilter === 'male' ? '男' : genderFilter === 'female' ? '女' : ''
    const parts = selectedDate.split('-')
    const year = parseInt(parts[0]) || 2026
    const month = parseInt(parts[1]) || 1
    const day = parseInt(parts[2]) || 1
    const titleText = `星嗨艺创${genderText}主播 ${year}年${month}月${day}日时长表`

    // 计算列数和列宽 - 根据显示的列动态分配宽度
    const columns: { key: string; label: string; width: number }[] = []
    const visibleColumns = []
    if (showRank) visibleColumns.push({ key: 'rank', label: '序号', baseWidth: 0.10 })
    if (showName) visibleColumns.push({ key: 'name', label: '主播姓名', baseWidth: 0.20 })
    if (showDuration) visibleColumns.push({ key: 'duration', label: `${day}号时长`, baseWidth: 0.50 })
    if (showRange) visibleColumns.push({ key: 'reward', label: '奖励', baseWidth: 0.20 })

    // 根据可见列的数量重新分配宽度，使总宽度为1.0
    let totalBaseWidth = 0
    visibleColumns.forEach(col => totalBaseWidth += col.baseWidth)

    visibleColumns.forEach(col => {
      columns.push({
        key: col.key,
        label: col.label,
        width: (col.baseWidth / totalBaseWidth) // 归一化到1.0
      })
    })

    // 计算画布宽度 - 保持固定宽度
    const containerWidth = Math.max(600 * scale, 750 * scale)

    // 计算高度
    const totalHeight = headerHeight + tableHeaderHeight + (sortedData.length * rowHeight) + footerHeight + rangeLegendHeight + padding * 2

    canvas.width = containerWidth
    canvas.height = totalHeight

    // 背景色
    ctx.fillStyle = '#F0F9FF'
    ctx.fillRect(0, 0, containerWidth, totalHeight)

    let currentY = padding

    // 标题栏
    ctx.fillStyle = '#0C4A6E'
    ctx.fillRect(0, currentY, containerWidth, headerHeight)
    
    ctx.fillStyle = '#f0f9ff'
    ctx.font = `bold ${22 * scale}px sans-serif`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(titleText, containerWidth / 2, currentY + headerHeight / 2)
    currentY += headerHeight

    // 时长范围图例 - 始终显示（当有奖励配置时）
    const validRanges = durationRanges.filter(r => r.minHours > 0 && r.reward)
    if (validRanges.length > 0) {
      const sortedRanges = [...validRanges].sort((a, b) => b.minHours - a.minHours)
      ctx.fillStyle = '#E0F2FE'
      ctx.fillRect(0, currentY, containerWidth, rangeLegendHeight)
      
      ctx.font = `${11 * scale}px sans-serif`
      ctx.fillStyle = '#0369A1'
      ctx.textAlign = 'left'
      
      const legendText = sortedRanges.map(r => `${r.minHours}小时+奖${r.reward}元`).join('  |  ')
      ctx.fillText(legendText, padding, currentY + rangeLegendHeight / 2 + 4 * scale)
      currentY += rangeLegendHeight
    }

    // 表头
    ctx.fillStyle = '#E0F2FE'
    ctx.fillRect(0, currentY, containerWidth, tableHeaderHeight)

    ctx.fillStyle = '#0369A1'
    ctx.font = `bold ${13 * scale}px sans-serif`
    ctx.textBaseline = 'middle'

    columns.forEach((col, index) => {
      const x = columns.slice(0, index).reduce((sum, c) => sum + c.width * containerWidth, 0)
      const width = col.width * containerWidth
      const centerX = x + width / 2

      if (col.key === 'name') {
        ctx.textAlign = 'left'
        ctx.fillText(col.label, x + width * 0.1, currentY + tableHeaderHeight / 2)
      } else if (col.key === 'duration') {
        ctx.textAlign = 'right'
        ctx.fillText(col.label, x + width - width * 0.08, currentY + tableHeaderHeight / 2)
      } else {
        ctx.textAlign = 'center'
        ctx.fillText(col.label, centerX, currentY + tableHeaderHeight / 2)
      }
    })
    currentY += tableHeaderHeight

    // 计算最大时长值
    const maxDuration = Math.max(...sortedData.filter(d => d.duration_minutes > 0).map(d => d.duration_minutes), 1)

    // 数据行
    sortedData.forEach((item, index) => {
      const actualRank = index + 1
      const isTop3 = actualRank <= 3
      const isInactive = item.duration_minutes <= 0
      const range = getDurationRange(item.duration_minutes, durationRanges)

      // 行背景
      if (isTop3 && actualRank === 1) {
        ctx.fillStyle = '#DBEAFE'
      } else if (isTop3 && actualRank === 2) {
        ctx.fillStyle = '#F0F9FF'
      } else if (isTop3 && actualRank === 3) {
        ctx.fillStyle = '#F8FAFC'
      } else if (range) {
        // 根据时长范围设置背景色
        const rangeIndex = [...durationRanges].sort((a, b) => b.minHours - a.minHours).findIndex(r => r.id === range.id)
        const colors = ['#DCFCE7', '#FEF3C7', '#DBEAFE', '#F3E8FF']
        ctx.fillStyle = colors[rangeIndex % colors.length] || '#FFFFFF'
      } else {
        ctx.fillStyle = '#FFFFFF'
      }
      ctx.fillRect(0, currentY, containerWidth, rowHeight)

      // 分割线
      ctx.strokeStyle = '#E0E7FF'
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
            ctx.fillStyle = '#1E3A5F'
            ctx.textAlign = 'center'
            ctx.fillText(icon, centerX, centerY)
          } else {
            ctx.font = `italic bold ${15 * scale}px serif`
            ctx.fillStyle = '#64748b'
            ctx.textAlign = 'center'
            ctx.fillText(actualRank.toString().padStart(2, '0'), centerX - 10 * scale, centerY)
          }
        } else if (col.key === 'name') {
          ctx.font = `${15 * scale}px sans-serif`
          ctx.fillStyle = '#1E3A5F'
          ctx.textAlign = 'left'
          ctx.fillText(item.anchor_name || item.anchor_id, colX + colWidth * 0.1, centerY)
        } else if (col.key === 'duration') {
          // 数据条
          if (!isInactive) {
            const barWidthRatio = item.duration_minutes / maxDuration
            const barMaxWidth = colWidth * 0.55
            const barWidth = barMaxWidth * barWidthRatio
            const barHeight = 20 * scale
            const barRight = colX + colWidth - colWidth * 0.05
            const barLeft = barRight - barWidth
            const barTop = currentY + (rowHeight - barHeight) / 2

            ctx.fillStyle = '#7DD3FC'
            ctx.beginPath()
            drawRoundRect(ctx, barLeft, barTop, barWidth, barHeight, 4 * scale)
            ctx.fill()
          }

          // 文字
          if (isInactive) {
            ctx.font = `bold ${14 * scale}px sans-serif`
            ctx.fillStyle = '#E11D48'
          } else {
            ctx.font = `${15 * scale}px sans-serif`
            ctx.fillStyle = '#1E3A5F'
          }
          ctx.textAlign = 'right'
          ctx.fillText(isInactive ? '未开播' : formatDuration(item.duration_minutes), colX + colWidth - colWidth * 0.08, centerY)
        } else if (col.key === 'reward') {
          if (range) {
            ctx.font = `bold ${14 * scale}px sans-serif`
            ctx.fillStyle = '#059669'
            ctx.textAlign = 'center'
            ctx.fillText(`¥${range.reward}`, centerX, centerY + 4 * scale)
          } else {
            ctx.font = `${12 * scale}px sans-serif`
            ctx.fillStyle = '#9CA3AF'
            ctx.textAlign = 'center'
            ctx.fillText('-', centerX, centerY)
          }
        }

        colX += colWidth
      })

      currentY += rowHeight
    })

    // 底部统计
    const inactiveStreamers = sortedData.filter(d => d.duration_minutes <= 0)
    if (inactiveStreamers.length > 0) {
      ctx.fillStyle = '#FEF2F2'
      ctx.fillRect(0, currentY, containerWidth, footerHeight)

      ctx.strokeStyle = '#FECACA'
      ctx.lineWidth = 0.5 * scale
      ctx.beginPath()
      ctx.moveTo(0, currentY)
      ctx.lineTo(containerWidth, currentY)
      ctx.stroke()

      const totalStreamers = sortedData.length
      const inactiveCount = inactiveStreamers.length

      ctx.fillStyle = '#DC2626'
      ctx.font = `bold ${24 * scale}px sans-serif`
      ctx.textAlign = 'center'
      ctx.fillText(totalStreamers.toString(), containerWidth / 2 - 100 * scale, currentY + 35 * scale)
      
      ctx.font = `${10 * scale}px sans-serif`
      ctx.fillText(`${genderText}主播人数`, containerWidth / 2 - 100 * scale, currentY + 55 * scale)

      ctx.font = `bold ${24 * scale}px sans-serif`
      ctx.fillText(inactiveCount.toString(), containerWidth / 2 + 100 * scale, currentY + 35 * scale)
      
      ctx.font = `${10 * scale}px sans-serif`
      ctx.fillText('未开播人数', containerWidth / 2 + 100 * scale, currentY + 55 * scale)

      const names = inactiveStreamers.map(d => d.anchor_name || d.anchor_id).join('、')
      const namesBgWidth = 300 * scale
      const namesBgHeight = 28 * scale
      const namesBgLeft = containerWidth / 2 - namesBgWidth / 2
      const namesBgTop = currentY + 75 * scale

      ctx.fillStyle = '#FFFFFF'
      ctx.beginPath()
      drawRoundRect(ctx, namesBgLeft, namesBgTop, namesBgWidth, namesBgHeight, 30 * scale)
      ctx.fill()

      ctx.fillStyle = '#DC2626'
      ctx.font = `bold ${14 * scale}px sans-serif`
      ctx.textAlign = 'center'
      ctx.fillText(names, containerWidth / 2, namesBgTop + namesBgHeight / 2 + 5 * scale)
    }
  }, [sortedData, genderFilter, showRank, showName, showDuration, showRange, selectedDate, durationRanges])

  const handleExport = async () => {
    const canvas = canvasRef.current
    if (!canvas) return

    setExporting(true)
    try {
      const link = document.createElement('a')
      const genderText = genderFilter === 'male' ? '男' : genderFilter === 'female' ? '女' : '全部'
      link.download = `${selectedDate}_${genderText}_时长_${sortedData.length}人.png`
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
      // 根据显示列动态生成CSV头部和行数据
      const headers = []
      const rowData = []

      if (showRank) headers.push('排名')
      if (showName) headers.push('主播ID', '主播姓名')
      if (showDuration) headers.push('时长(分钟)', '时长(格式)', '时长范围')
      if (showRange) headers.push('奖励')

      headers.push('日期')

      const rows = sortedData.map((stat, index) => {
        const row = []
        const range = getDurationRange(stat.duration_minutes, durationRanges)

        if (showRank) row.push(index + 1)
        if (showName) {
          row.push(stat.anchor_id)
          row.push(stat.anchor_name || '')
        }
        if (showDuration) {
          row.push(stat.duration_minutes)
          row.push(formatDuration(stat.duration_minutes))
          row.push(range ? `${range.minHours}小时+` : '-')
        }
        if (showRange) {
          row.push(range ? range.reward : '-')
        }

        row.push(selectedDate)
        return row
      })

      const csvContent = [headers, ...rows]
        .map(row => row.map(cell => `"${cell}"`).join(','))
        .join('\n')

      const genderText = genderFilter === 'male' ? '男' : genderFilter === 'female' ? '女' : '全部'
      const defaultFileName = `${selectedDate}_${genderText}_时长数据_${sortedData.length}人.csv`
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

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto py-4">
      <div className="bg-background-light rounded-xl w-[900px] max-h-[95vh] border border-slate-700 flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700 shrink-0">
          <h3 className="text-lg font-semibold text-text">导出时长表格图片</h3>
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
          <div className="mb-4 p-4 bg-slate-800 rounded-lg space-y-4">
            <div className="flex flex-wrap gap-4">
              {/* 日期选择 */}
              <div className="relative" ref={calendarRef}>
                <label className="text-text-muted text-sm block mb-2">选择日期</label>
                <div className="flex gap-2 items-center">
                  <button
                    onClick={() => handleQuickDate('today')}
                    className={`px-3 py-1 rounded text-sm ${
                      selectedDate === today 
                        ? 'bg-accent-blue text-black' 
                        : 'bg-slate-700 text-text hover:bg-slate-600'
                    }`}
                  >
                    今日
                  </button>
                  <button
                    onClick={() => handleQuickDate('yesterday')}
                    className={`px-3 py-1 rounded text-sm ${
                      selectedDate === yesterday 
                        ? 'bg-accent-blue text-black' 
                        : 'bg-slate-700 text-text hover:bg-slate-600'
                    }`}
                  >
                    前一天
                  </button>
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
                    
                    <div className="grid grid-cols-7 gap-1 mb-1">
                      {['日', '一', '二', '三', '四', '五', '六'].map(d => (
                        <div key={d} className="text-center text-xs text-text-muted py-1">{d}</div>
                      ))}
                    </div>
                    
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
                                  ? 'bg-accent-blue text-black'
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
                    className={`px-3 py-1 rounded text-sm ${genderFilter === 'all' ? 'bg-accent-blue text-black' : 'bg-slate-700 text-text'}`}
                  >
                    全部
                  </button>
                  <button
                    onClick={() => setGenderFilter('male')}
                    className={`px-3 py-1 rounded text-sm ${genderFilter === 'male' ? 'bg-accent-blue text-black' : 'bg-slate-700 text-text'}`}
                  >
                    男
                  </button>
                  <button
                    onClick={() => setGenderFilter('female')}
                    className={`px-3 py-1 rounded text-sm ${genderFilter === 'female' ? 'bg-accent-blue text-black' : 'bg-slate-700 text-text'}`}
                  >
                    女
                  </button>
                </div>
              </div>

              {/* 显示列 */}
              <div>
                <label className="text-text-muted text-sm block mb-2">显示列</label>
                <div className="flex gap-2 flex-wrap">
                  <label className="flex items-center gap-1 text-sm text-text">
                    <input type="checkbox" checked={showRank} onChange={(e) => setShowRank(e.target.checked)} className="accent-accent-blue" />
                    序号
                  </label>
                  <label className="flex items-center gap-1 text-sm text-text">
                    <input type="checkbox" checked={showName} onChange={(e) => setShowName(e.target.checked)} className="accent-accent-blue" />
                    姓名
                  </label>
                  <label className="flex items-center gap-1 text-sm text-text">
                    <input type="checkbox" checked={showDuration} onChange={(e) => setShowDuration(e.target.checked)} className="accent-accent-blue" />
                    时长
                  </label>
                  <label className="flex items-center gap-1 text-sm text-text">
                    <input type="checkbox" checked={showRange} onChange={(e) => setShowRange(e.target.checked)} className="accent-accent-blue" />
                    奖励
                  </label>
                </div>
              </div>
            </div>

            {/* 时长范围配置 */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <label className="text-text-muted text-sm">时长范围配置（小时）</label>
                <button
                  onClick={addDurationRange}
                  className="px-2 py-1 text-xs bg-accent-green/20 text-accent-green rounded hover:bg-accent-green/30"
                >
                  + 添加范围
                </button>
              </div>
              <div className="flex flex-wrap gap-2">
                {durationRanges.map((range, index) => (
                  <div key={range.id} className="flex items-center gap-1 bg-slate-700 rounded px-2 py-1">
                    <span className="text-text-muted text-xs">≥</span>
                    <input
                      type="number"
                      value={range.minHours}
                      onChange={(e) => updateDurationRange(range.id, 'minHours', e.target.value)}
                      className="w-14 bg-slate-600 text-text text-sm px-2 py-1 rounded text-center"
                      min="0"
                      step="0.5"
                    />
                    <span className="text-text-muted text-xs">小时</span>
                    <span className="text-text-muted text-xs ml-1">奖</span>
                    <input
                      type="text"
                      value={range.reward}
                      onChange={(e) => updateDurationRange(range.id, 'reward', e.target.value)}
                      className="w-16 bg-slate-600 text-text text-sm px-2 py-1 rounded"
                      placeholder="金额"
                    />
                    <span className="text-text-muted text-xs">元</span>
                    <button
                      onClick={() => removeDurationRange(range.id)}
                      className="ml-1 text-red-400 hover:text-red-300"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  </div>
                ))}
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
            className="flex-1 px-4 py-2 bg-accent-blue hover:bg-blue-500 disabled:opacity-50 rounded-lg text-black font-medium transition-colors flex items-center justify-center gap-2"
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

export default DurationTableImageExport
