import { useState, useEffect, useRef } from 'react'
import Papa from 'papaparse'
import { Anchor } from '../types'
import Toast from './Toast'

interface ImportModalProps {
  type: 'wave' | 'duration'
  anchors: Anchor[]
  dropFilePath?: string
  onClose: () => void
  onSuccess: () => void
  onImportStart?: () => void
  onImportComplete?: () => void
}

interface PreviewRow {
  anchor_id: string
  anchor_name: string
  value: string | number
  matched: boolean // 是否匹配到主播表
}

interface DateRange {
  start: string
  end: string
}

// 快捷日期范围类型
type QuickDateRange = 'none' | 'thisMonth' | 'firstHalf' | 'secondHalf' | 'lastMonth' | 'last7Days' | 'last15Days'

function ImportModal({ type, anchors, dropFilePath, onClose, onSuccess, onImportStart, onImportComplete }: ImportModalProps) {
  const [step, setStep] = useState<'select' | 'preview' | 'importing'>('select')
  const [previewData, setPreviewData] = useState<PreviewRow[]>([])
  const [importDate, setImportDate] = useState(new Date().toISOString().split('T')[0])
  const [dateRange, setDateRange] = useState<DateRange>({ start: '', end: '' })
  const [useDateRange, setUseDateRange] = useState(false)
  const [quickDateRange, setQuickDateRange] = useState<QuickDateRange>('none')
  const [error, setError] = useState('')
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null)
  const [durationPreviewData, setDurationPreviewData] = useState<PreviewRow[]>([]) // 时长数据预览

  // 创建锚点映射，支持前8位匹配
  const anchorMap = new Map(anchors.map(a => [a.anchor_id, a]))
  const anchorMapByFirst8 = new Map(anchors.map(a => [a.anchor_id.substring(0, 8), a]))
  const processedDropFile = useRef<string>('') // 防止重复处理

  // 计算快捷日期范围
  const getQuickDateRange = (type: QuickDateRange): DateRange => {
    const now = new Date()
    const year = now.getFullYear()
    const month = now.getMonth()
    
    switch (type) {
      case 'thisMonth': {
        // 本月
        const lastDay = new Date(year, month + 1, 0).getDate()
        const startDate = new Date(year, month, 1)
        const endDate = new Date(year, month, lastDay)
        return {
          start: startDate.toISOString().split('T')[0],
          end: endDate.toISOString().split('T')[0]
        }
      }
      case 'firstHalf': {
        // 上半月（1-15号）
        const startDate = new Date(year, month, 1)
        const endDate = new Date(year, month, 15)
        return {
          start: startDate.toISOString().split('T')[0],
          end: endDate.toISOString().split('T')[0]
        }
      }
      case 'secondHalf': {
        // 下半月（16号到月底）
        const lastDay = new Date(year, month + 1, 0).getDate()
        const startDate = new Date(year, month, 16)
        const endDate = new Date(year, month, lastDay)
        return {
          start: startDate.toISOString().split('T')[0],
          end: endDate.toISOString().split('T')[0]
        }
      }
      case 'lastMonth': {
        // 上个月
        const startDate = new Date(year, month - 1, 1)
        const lastDayOfLastMonth = new Date(year, month, 0).getDate()
        const endDate = new Date(year, month - 1, lastDayOfLastMonth)
        return {
          start: startDate.toISOString().split('T')[0],
          end: endDate.toISOString().split('T')[0]
        }
      }
      case 'last7Days': {
        // 最近7天
        const endDate = new Date()
        const startDate = new Date()
        startDate.setDate(startDate.getDate() - 6)
        return {
          start: startDate.toISOString().split('T')[0],
          end: endDate.toISOString().split('T')[0]
        }
      }
      case 'last15Days': {
        // 最近15天
        const endDate = new Date()
        const startDate = new Date()
        startDate.setDate(startDate.getDate() - 14)
        return {
          start: startDate.toISOString().split('T')[0],
          end: endDate.toISOString().split('T')[0]
        }
      }
      default:
        return { start: '', end: '' }
    }
  }

  // 应用快捷日期范围
  const applyQuickDateRange = (type: QuickDateRange) => {
    setQuickDateRange(type)
    if (type !== 'none') {
      const range = getQuickDateRange(type)
      setDateRange(range)
      setUseDateRange(true)
    }
  }

  // 处理文件内容
  const processFileContent = (content: string, filePath?: string) => {
    setError('')
    setDurationPreviewData([])

    Papa.parse(content, {
      header: true,
      skipEmptyLines: true,
      complete: (results) => {
        try {
          const data = results.data as any[]
          
          // CSV 数据验证
          const validationErrors: string[] = []
          
          if (data.length === 0) {
            validationErrors.push('CSV 文件为空或格式不正确')
          }
          
          // 检测文件类型（通过列名判断）
          const headers = Object.keys(data[0] || {})
          const hasWaveColumn = headers.includes('音浪') // 当日音浪
          const hasTotalWaveColumn = headers.includes('总音浪') // 主播概览列表格式
          const hasDurationColumn = headers.includes('开播有效时长')
          const hasRankColumn = headers.includes('排名') // 主播榜格式
          
          // 检查必要字段
          if (type === 'wave') {
            if (!hasWaveColumn && !hasTotalWaveColumn) {
              validationErrors.push('缺少"音浪"或"总音浪"列')
            }
          } else {
            if (!hasDurationColumn) {
              validationErrors.push('缺少"开播有效时长"列')
            }
          }
          
          // 检查主播ID字段
          if (!headers.includes('主播ID')) {
            validationErrors.push('缺少"主播ID"列')
          }
          
          if (validationErrors.length > 0) {
            setError(`CSV 数据验证失败:\n${validationErrors.join('\n')}`)
            return
          }
          
          if (type === 'wave') {
            // 音浪导入：优先使用"音浪"列，其次使用"总音浪"列
            const waveColumnName = hasWaveColumn ? '音浪' : '总音浪'
            const preview: PreviewRow[] = data
              .filter(row => row['主播ID'])
              .map(row => {
                const anchorId = String(row['主播ID']).trim()
                let anchor = anchorMap.get(anchorId)
                // 如果精确匹配失败，尝试前8位匹配
                if (!anchor) {
                  anchor = anchorMapByFirst8.get(anchorId.substring(0, 8))
                }
                const waveStr = String(row[waveColumnName] || '0')
                // 支持格式：42370音浪 或 42370
                const waveValue = parseInt(waveStr.replace(/[^\d]/g, '').replace(/,/g, '')) || 0
                return {
                  anchor_id: anchorId,
                  anchor_name: anchor?.anchor_name || row['主播名'] || row['主播昵称'] || '-',
                  value: waveValue,
                  matched: !!anchor
                }
              })
              .filter(row => row.matched && Number(row.value) > 0)
            
            setPreviewData(preview)
            
            // 如果文件同时包含开播有效时长，也预览时长数据
            if (hasDurationColumn) {
              const durationPreview: PreviewRow[] = data
                .filter(row => row['主播ID'] && row['开播有效时长'])
                .map(row => {
                  const anchorId = String(row['主播ID']).trim()
                  const anchor = anchorMap.get(anchorId)
                  return {
                    anchor_id: anchorId,
                    anchor_name: anchor?.anchor_name || row['主播名'] || row['主播昵称'] || '-',
                    value: String(row['开播有效时长']),
                    matched: !!anchor
                  }
                })
                .filter(row => row.matched)
              
              setDurationPreviewData(durationPreview)
            }
          } else {
            // 时长导入：只显示主播名单中的数据
            const preview: PreviewRow[] = data
              .filter(row => row['主播ID'] && row['开播有效时长'])
              .map(row => {
                const anchorId = String(row['主播ID']).trim()
                let anchor = anchorMap.get(anchorId)
                let matchedAnchorId = anchorId
                // 如果精确匹配失败，尝试前8位匹配
                if (!anchor) {
                  anchor = anchorMapByFirst8.get(anchorId.substring(0, 8))
                  if (anchor) {
                    matchedAnchorId = anchor.anchor_id // 使用主播表中的实际ID
                  }
                }
                return {
                  anchor_id: matchedAnchorId, // 使用匹配到的主播表中的ID
                  anchor_name: anchor?.anchor_name || row['主播名'] || row['主播昵称'] || '-',
                  value: String(row['开播有效时长']),
                  matched: !!anchor
                }
              })
              .filter(row => row.matched)
            
            setPreviewData(preview)
          }
          setStep('preview')
        } catch (err) {
          setError('解析CSV文件失败: ' + String(err))
        }
      },
      error: () => {
        setError('解析CSV文件失败')
      }
    })
  }

  // 处理拖拽导入的文件
  useEffect(() => {
    if (dropFilePath && dropFilePath !== processedDropFile.current) {
      processedDropFile.current = dropFilePath
      const loadDroppedFile = async () => {
        try {
          const result = await window.electronAPI.readFile(dropFilePath)
          if (result.success && result.data) {
            processFileContent(result.data, dropFilePath)
          } else {
            setError('读取文件失败: ' + result.error)
          }
        } catch (err) {
          setError('读取文件失败: ' + String(err))
        }
      }
      loadDroppedFile()
    }
  }, [dropFilePath, type])

  const handleSelectFile = async () => {
    try {
      setError('')
      setDurationPreviewData([])

      const filePaths = await window.electronAPI.openFileDialog([
        { name: 'CSV Files', extensions: ['csv'] }
      ])

      if (!filePaths || filePaths.length === 0) return

      const path = filePaths[0]
      const result = await window.electronAPI.readFile(path)
      
      if (!result.success || !result.data) {
        setError('读取文件失败: ' + result.error)
        return
      }

      processFileContent(result.data, path)
    } catch (err) {
      setError('选择文件失败')
    }
  }

  // 生成日期范围数组
  const generateDateRange = (start: string, end: string): string[] => {
    const dates: string[] = []
    const startDate = new Date(start)
    const endDate = new Date(end)
    
    while (startDate <= endDate) {
      dates.push(startDate.toISOString().split('T')[0])
      startDate.setDate(startDate.getDate() + 1)
    }
    
    return dates
  }

  const handleConfirmImport = async () => {
    // 只导入匹配主播表的数据
    const matchedData = previewData.filter(row => row.matched)
    
    if (matchedData.length === 0) {
      setToast({ message: '没有可导入的数据（未匹配到主播）', type: 'error' })
      return
    }

    setStep('importing')
    setError('')

    // 通知父组件导入开始
    if (onImportStart) onImportStart()

    try {
      if (type === 'wave') {
        // 音浪导入：支持单日和日期范围
        let dates: string[] = []
        
        if (useDateRange && dateRange.start && dateRange.end) {
          dates = generateDateRange(dateRange.start, dateRange.end)
        } else {
          dates = [importDate]
        }

        // 生成音浪数据：如果是单日导入，直接使用原始值；如果是范围导入，总量数据放到第一天
        const waveData = []
        for (const row of matchedData) {
          if (dates.length === 1) {
            // 单日导入：使用原始值
            waveData.push({
              anchor_id: row.anchor_id,
              date: dates[0],
              wave_value: Number(row.value),
              rank: row.matched ? matchedData.findIndex(r => r.anchor_id === row.anchor_id) + 1 : 0
            })
          } else {
            // 范围导入：总量数据放到第一天，后续日期保持0或被后续导入覆盖
            waveData.push({
              anchor_id: row.anchor_id,
              date: dates[0], // 第一天放总量
              wave_value: Number(row.value), // 总量数据
              rank: row.matched ? matchedData.findIndex(r => r.anchor_id === row.anchor_id) + 1 : 0
            })
            // 其他日期不创建数据，让后续的单日导入可以独立存在
          }
        }

        const result = await window.electronAPI.addWaveStats(waveData)
        
        if (result && result.success) {
          // 如果同时有时长数据，也导入时长
          if (durationPreviewData.length > 0) {
            // 时长数据处理：如果是单日导入，使用原始值；如果是范围导入，总量数据放到第一天
            const durationDates = useDateRange && dateRange.start && dateRange.end ? generateDateRange(dateRange.start, dateRange.end) : [importDate]
            const durationData = []
            
            for (const row of durationPreviewData) {
              if (durationDates.length === 1) {
                // 单日导入：使用原始时长值
                const durationStr = String(row.value)
                const hourMatch = durationStr.match(/(\d+)小时/)
                const minMatch = durationStr.match(/(\d+)分钟/)
                const secMatch = durationStr.match(/(\d+)秒/)
                const hours = hourMatch ? parseInt(hourMatch[1]) : 0
                const minutes = minMatch ? parseInt(minMatch[1]) : 0
                const seconds = secMatch ? parseInt(secMatch[1]) : 0
                const totalMinutes = hours * 60 + minutes + Math.round(seconds / 60)
                
                durationData.push({
                  anchor_id: row.anchor_id,
                  date: durationDates[0],
                  duration_minutes: totalMinutes
                })
              } else {
                // 范围导入：总量数据放到第一天
                const durationStr = String(row.value)
                const hourMatch = durationStr.match(/(\d+)小时/)
                const minMatch = durationStr.match(/(\d+)分钟/)
                const secMatch = durationStr.match(/(\d+)秒/)
                const hours = hourMatch ? parseInt(hourMatch[1]) : 0
                const minutes = minMatch ? parseInt(minMatch[1]) : 0
                const seconds = secMatch ? parseInt(secMatch[1]) : 0
                const totalMinutes = hours * 60 + minutes + Math.round(seconds / 60)
                
                durationData.push({
                  anchor_id: row.anchor_id,
                  date: durationDates[0], // 第一天放总量
                  duration_minutes: totalMinutes // 总量数据
                })
                // 其他日期不创建数据，让后续的单日导入可以独立存在
              }
            }
            
            await window.electronAPI.addDurationStats(durationData)
            const rangeText = useDateRange && dateRange.start && dateRange.end 
              ? `（${dateRange.start} 至 ${dateRange.end} 共${dates.length}天）` 
              : `（${importDate}）`
            setToast({ message: `导入成功！已保存 ${waveData.length} 条音浪数据和 ${durationData.length} 条时长数据${rangeText}，正在后台同步...`, type: 'success' })
          } else {
            const rangeText = useDateRange && dateRange.start && dateRange.end 
              ? `（${dateRange.start} 至 ${dateRange.end} 共${dates.length}天）` 
              : `（${importDate}）`
            setToast({ message: `导入成功！已保存 ${waveData.length} 条音浪数据${rangeText}，正在后台同步...`, type: 'success' })
          }
          setTimeout(() => {
            onSuccess()
          }, 1500)
        } else {
          const errMsg = result?.error || '未知错误'
          setError('导入失败: ' + errMsg)
          setStep('preview')
        }
      } else {
        // 时长导入：支持日期范围
        let dates: string[] = []
        
        if (useDateRange && dateRange.start && dateRange.end) {
          dates = generateDateRange(dateRange.start, dateRange.end)
        } else {
          dates = [importDate]
        }

        // 时长是累计数据，直接存储到选择的日期或范围结束日期
        const durationData = matchedData.map(row => {
          const durationStr = String(row.value)
          const hourMatch = durationStr.match(/(\d+)小时/)
          const minMatch = durationStr.match(/(\d+)分钟/)
          const secMatch = durationStr.match(/(\d+)秒/)
          const hours = hourMatch ? parseInt(hourMatch[1]) : 0
          const minutes = minMatch ? parseInt(minMatch[1]) : 0
          const seconds = secMatch ? parseInt(secMatch[1]) : 0
          return {
            anchor_id: row.anchor_id,
            // 如果是日期范围，存储到结束日期；否则存储到选择的日期
            date: useDateRange && dateRange.end ? dateRange.end : importDate,
            duration_minutes: hours * 60 + minutes + Math.round(seconds / 60)
          }
        })

        const result = await window.electronAPI.addDurationStats(durationData)
        
        if (result && result.success) {
          const rangeText = useDateRange && dateRange.start && dateRange.end 
            ? `（${dateRange.start} 至 ${dateRange.end} 累计）` 
            : ''
          setToast({ message: `导入成功！已保存 ${durationData.length} 条时长数据${rangeText}，正在后台同步...`, type: 'success' })
          setTimeout(() => {
            onSuccess()
          }, 1500)
        } else {
          const errMsg = result?.error || '未知错误'
          setError('导入失败: ' + errMsg)
          setStep('preview')
        }
      }
    } catch (err: any) {
      const errMsg = err?.message || String(err)
      setError('导入异常: ' + errMsg)
      setStep('preview')
    } finally {
      // 通知父组件导入完成
      if (onImportComplete) onImportComplete()
    }
  }

  const formatDuration = (durationStr: string) => {
    const hourMatch = durationStr.match(/(\d+)小时/)
    const minMatch = durationStr.match(/(\d+)分钟/)
    if (hourMatch && minMatch) {
      return `${hourMatch[1]}小时${minMatch[1]}分`
    }
    return durationStr
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-background-light rounded-xl w-[900px] max-h-[90vh] border border-slate-700 flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700 flex-shrink-0">
          <h3 className="text-lg font-semibold text-text">
            导入{type === 'wave' ? '音浪' : '时长'}数据
          </h3>
          <button
            onClick={onClose}
            className="text-text-muted hover:text-text transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="p-6 flex-1 overflow-hidden">
          {step === 'select' && (
            <div
              onClick={handleSelectFile}
              className="border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-colors border-slate-600 hover:border-primary hover:bg-slate-800/30"
            >
              <svg className="w-12 h-12 mx-auto text-text-muted mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
              </svg>
              <p className="text-text font-medium">点击选择CSV文件</p>
              <p className="text-text-muted text-sm mt-1">
                {type === 'wave'
                  ? '支持格式：主播榜、主播概览列表等'
                  : '请选择时长CSV文件（主播概览列表格式）'}
              </p>
            </div>
          )}

          {step === 'preview' && (
            <div className="space-y-4 h-full flex flex-col">
              {/* 日期选择区域 */}
              <div className="flex items-start gap-6 flex-shrink-0">
                <div className="space-y-3">
                  <div className="flex items-center gap-4">
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="radio"
                        checked={!useDateRange}
                        onChange={() => { setUseDateRange(false); setQuickDateRange('none') }}
                        className="w-4 h-4 text-primary"
                      />
                      <span className="text-sm text-text">单日导入</span>
                    </label>
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="radio"
                        checked={useDateRange}
                        onChange={() => setUseDateRange(true)}
                        className="w-4 h-4 text-primary"
                      />
                      <span className="text-sm text-text">日期范围导入</span>
                    </label>
                  </div>
                  
                  {!useDateRange ? (
                    <div>
                      <label className="block text-sm text-text-muted mb-1">导入日期</label>
                      <input
                        type="date"
                        value={importDate}
                        onChange={(e) => setImportDate(e.target.value)}
                        className="px-4 py-2 bg-background border border-slate-700 rounded-lg text-text focus:outline-none focus:border-primary"
                      />
                    </div>
                  ) : (
                    <div className="space-y-3">
                      {/* 快捷日期选择按钮 */}
                      <div>
                        <label className="block text-sm text-text-muted mb-2">快捷选择</label>
                        <div className="flex flex-wrap gap-2">
                          <button
                            onClick={() => applyQuickDateRange('thisMonth')}
                            className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
                              quickDateRange === 'thisMonth'
                                ? 'bg-primary text-white'
                                : 'bg-slate-700 text-text-muted hover:text-text hover:bg-slate-600'
                            }`}
                          >
                            本月
                          </button>
                          <button
                            onClick={() => applyQuickDateRange('firstHalf')}
                            className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
                              quickDateRange === 'firstHalf'
                                ? 'bg-primary text-white'
                                : 'bg-slate-700 text-text-muted hover:text-text hover:bg-slate-600'
                            }`}
                          >
                            上半月(1-15号)
                          </button>
                          <button
                            onClick={() => applyQuickDateRange('secondHalf')}
                            className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
                              quickDateRange === 'secondHalf'
                                ? 'bg-primary text-white'
                                : 'bg-slate-700 text-text-muted hover:text-text hover:bg-slate-600'
                            }`}
                          >
                            下半月(16-月底)
                          </button>
                          <button
                            onClick={() => applyQuickDateRange('lastMonth')}
                            className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
                              quickDateRange === 'lastMonth'
                                ? 'bg-primary text-white'
                                : 'bg-slate-700 text-text-muted hover:text-text hover:bg-slate-600'
                            }`}
                          >
                            上个月
                          </button>
                          <button
                            onClick={() => applyQuickDateRange('last7Days')}
                            className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
                              quickDateRange === 'last7Days'
                                ? 'bg-primary text-white'
                                : 'bg-slate-700 text-text-muted hover:text-text hover:bg-slate-600'
                            }`}
                          >
                            最近7天
                          </button>
                          <button
                            onClick={() => applyQuickDateRange('last15Days')}
                            className={`px-3 py-1.5 text-sm rounded-lg transition-colors ${
                              quickDateRange === 'last15Days'
                                ? 'bg-primary text-white'
                                : 'bg-slate-700 text-text-muted hover:text-text hover:bg-slate-600'
                            }`}
                          >
                            最近15天
                          </button>
                        </div>
                      </div>
                      
                      {/* 自定义日期范围 */}
                      <div>
                        <label className="block text-sm text-text-muted mb-1">自定义日期范围（数据将平均分配到每天）</label>
                        <div className="flex items-center gap-2">
                          <input
                            type="date"
                            value={dateRange.start}
                            onChange={(e) => { setDateRange({ ...dateRange, start: e.target.value }); setQuickDateRange('none') }}
                            className="px-4 py-2 bg-background border border-slate-700 rounded-lg text-text focus:outline-none focus:border-primary"
                          />
                          <span className="text-text-muted">至</span>
                          <input
                            type="date"
                            value={dateRange.end}
                            onChange={(e) => { setDateRange({ ...dateRange, end: e.target.value }); setQuickDateRange('none') }}
                            className="px-4 py-2 bg-background border border-slate-700 rounded-lg text-text focus:outline-none focus:border-primary"
                          />
                        </div>
                      </div>
                    </div>
                  )}
                </div>
                
                {/* 统计信息 */}
                <div className="flex-1 text-right space-y-1">
                  <div className="text-text-muted text-sm">
                    共 <span className="text-green-400 font-medium">{previewData.length}</span> 条{type === 'wave' ? '音浪' : '时长'}数据
                  </div>
                  {durationPreviewData.length > 0 && type === 'wave' && (
                    <div className="text-text-muted text-sm">
                      同时将导入 <span className="text-blue-400 font-medium">{durationPreviewData.length}</span> 条时长数据
                    </div>
                  )}
                </div>
              </div>

              {/* 数据预览表格 */}
              <div className="border border-slate-700 rounded-lg overflow-hidden flex-1 flex flex-col">
                <div className="overflow-auto flex-1">
                  <table className="w-full">
                    <thead className="bg-slate-800/50 sticky top-0 z-10">
                      <tr className="text-left text-text-muted text-sm">
                        <th className="px-4 py-3 font-medium whitespace-nowrap">主播ID</th>
                        <th className="px-4 py-3 font-medium whitespace-nowrap">主播姓名</th>
                        <th className="px-4 py-3 font-medium whitespace-nowrap">{type === 'wave' ? '音浪' : '时长'}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {previewData.map((row, index) => (
                        <tr 
                          key={index} 
                          className="border-t border-slate-700/50"
                        >
                          <td className="px-4 py-2 text-text-muted font-mono text-sm whitespace-nowrap">{row.anchor_id}</td>
                          <td className="px-4 py-2 text-text whitespace-nowrap">{row.anchor_name}</td>
                          <td className="px-4 py-2 text-accent-gold font-mono whitespace-nowrap">
                            {type === 'wave' 
                              ? Number(row.value).toLocaleString() 
                              : formatDuration(String(row.value))}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {step === 'importing' && (
            <div className="flex flex-col items-center justify-center py-12">
              <div className="w-12 h-12 border-2 border-primary border-t-transparent rounded-full animate-spin mb-4" />
              <p className="text-text-muted">正在导入数据...</p>
              <p className="text-text-muted text-sm mt-2">数据将保存到本地并同步到远程数据库</p>
            </div>
          )}

          {error && (
            <div className="mt-4 p-3 bg-red-500/20 border border-red-500/50 rounded-lg text-red-400 text-sm">
              {error}
            </div>
          )}
        </div>

        {step === 'preview' && (
          <div className="flex gap-3 px-6 py-4 border-t border-slate-700 flex-shrink-0">
            <button
              onClick={() => setStep('select')}
              className="flex-1 px-4 py-2 border border-slate-600 rounded-lg text-text-muted hover:bg-slate-800 transition-colors"
            >
              重新选择
            </button>
            <button
              onClick={handleConfirmImport}
              disabled={previewData.length === 0}
              className="flex-1 px-4 py-2 bg-primary hover:bg-primary-light disabled:opacity-50 rounded-lg text-white font-medium transition-colors"
            >
              确认导入 ({previewData.length} 条)
            </button>
          </div>
        )}
      </div>
      
      {/* Toast 提示 */}
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  )
}

export default ImportModal
