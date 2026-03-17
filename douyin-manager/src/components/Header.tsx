import { useState, useMemo, useEffect } from 'react'
import { Anchor, TimeRange } from '../types'

interface HeaderProps {
  onSyncAnchors: () => void
  loading: boolean
  isSyncingData?: boolean // 是否正在同步数据
  currentPage: string
  timeRange: TimeRange
  customDateRange: { start: string; end: string }
  onTimeRangeChange: (range: TimeRange) => void
  onCustomDateChange: (range: { start: string; end: string }) => void
  anchors: Anchor[]
  waveStats: { anchor_id: string; anchor_name?: string; date: string }[]
  durationStats: { anchor_id: string; anchor_name?: string; date: string }[]
  selectedAnchorIds: string[]
  onAnchorSelectionChange: (ids: string[]) => void
  anchorListSearchTerm: string
  onAnchorListSearchChange: (term: string) => void
  onAddAnchor: () => void
  onImportAnchors: () => void
  selectedYearMonth: string
  onYearMonthChange: (ym: string) => void
  onExport: (type: 'wave' | 'duration') => void
}

function Header({ 
  onSyncAnchors, 
  loading, 
  isSyncingData = false, // 默认为false
  currentPage,
  timeRange,
  customDateRange,
  onTimeRangeChange,
  onCustomDateChange,
  anchors,
  waveStats,
  durationStats,
  selectedAnchorIds,
  onAnchorSelectionChange,
  anchorListSearchTerm,
  onAnchorListSearchChange,
  onAddAnchor,
  onImportAnchors,
  selectedYearMonth,
  onYearMonthChange
}: HeaderProps) {
  // 主播筛选状态
  const [showAnchorSelector, setShowAnchorSelector] = useState(false)
  const [anchorSearchTerm, setAnchorSearchTerm] = useState('')
  
  // 窗口控制状态
  const [isMaximized, setIsMaximized] = useState(false)

  useEffect(() => {
    const checkMaximized = async () => {
      try {
        const maximized = await window.electronAPI.windowIsMaximized()
        setIsMaximized(maximized)
      } catch (error) {
        console.error('Failed to check window maximized state:', error)
        setIsMaximized(false)
      }
    }
    checkMaximized()
  }, [])

  const handleMaximize = async () => {
    await window.electronAPI.windowMaximize()
    const maximized = await window.electronAPI.windowIsMaximized()
    setIsMaximized(maximized)
  }

  const handleClose = () => {
    window.electronAPI.windowClose()
  }

  // 创建主播ID到名字的映射
  const anchorMap = useMemo(() => {
    const map = new Map<string, string>()
    anchors.forEach(a => map.set(a.anchor_id, a.anchor_name || a.anchor_id))
    waveStats.forEach(s => {
      if (s.anchor_name && !map.has(s.anchor_id)) {
        map.set(s.anchor_id, s.anchor_name)
      }
    })
    durationStats.forEach(s => {
      if (s.anchor_name && !map.has(s.anchor_id)) {
        map.set(s.anchor_id, s.anchor_name)
      }
    })
    return map
  }, [anchors, waveStats, durationStats])

  // 获取所有涉及的主播ID
  const allAnchorIds = useMemo(() => {
    const ids = new Set<string>()
    waveStats.forEach(s => ids.add(s.anchor_id))
    durationStats.forEach(s => ids.add(s.anchor_id))
    return Array.from(ids)
  }, [waveStats, durationStats])

  // 筛选后的主播列表
  const filteredAnchors = useMemo(() => {
    if (!anchorSearchTerm) return allAnchorIds
    return allAnchorIds.filter(id => {
      const name = anchorMap.get(id) || id
      return name.toLowerCase().includes(anchorSearchTerm.toLowerCase()) || id.includes(anchorSearchTerm)
    })
  }, [allAnchorIds, anchorSearchTerm, anchorMap])

  const toggleAnchor = (anchorId: string) => {
    if (selectedAnchorIds.includes(anchorId)) {
      onAnchorSelectionChange(selectedAnchorIds.filter(id => id !== anchorId))
    } else {
      onAnchorSelectionChange([...selectedAnchorIds, anchorId])
    }
  }

  const selectAllAnchors = () => {
    onAnchorSelectionChange(allAnchorIds)
  }

  const clearAnchorSelection = () => {
    onAnchorSelectionChange([])
  }

  // 是否显示筛选按钮（只在数据统计页面显示）
  const showFilters = currentPage === 'stats'
  // 是否显示主播列表搜索（只在主播列表页面显示）
  const showAnchorListSearch = currentPage === 'anchors'
  // 是否显示数据中心年月选择（只在数据中心页面显示）
  const showDataCenterYearMonth = currentPage === 'datacenter'

  // 格式化日期
  const formatDateStr = (date: Date | string): string => {
    if (!date) return ''
    if (typeof date === 'string') return date
    return date.toISOString().split('T')[0]
  }

  // 获取可用的年月列表（用于数据中心）
  const availableYearMonths = useMemo(() => {
    const yearMonthSet = new Set<string>()
    waveStats.forEach(stat => {
      const date = formatDateStr(stat.date)
      if (date) {
        const [year, month] = date.split('-')
        yearMonthSet.add(`${year}-${month}`)
      }
    })
    durationStats.forEach(stat => {
      const date = formatDateStr(stat.date)
      if (date) {
        const [year, month] = date.split('-')
        yearMonthSet.add(`${year}-${month}`)
      }
    })
    return Array.from(yearMonthSet).sort().reverse()
  }, [waveStats, durationStats])

  return (
    <header className="fixed top-0 left-0 right-0 h-16 bg-background-light border-b border-slate-700 flex items-center justify-between px-6 z-50">
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-gradient-to-br from-pink-500 to-black flex items-center justify-center">
            {/* 抖音图标 */}
            <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
              <path d="M19.59 6.69a4.83 4.83 0 0 1-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 0 1-5.2 1.74 2.89 2.89 0 0 1 2.31-4.64 2.93 2.93 0 0 1 .88.13V9.4a6.84 6.84 0 0 0-1-.05A6.33 6.33 0 0 0 5 20.1a6.34 6.34 0 0 0 10.86-4.43v-7a8.16 8.16 0 0 0 4.77 1.52v-3.4a4.85 4.85 0 0 1-1-.1z"/>
            </svg>
          </div>
          <h1 className="text-xl font-semibold text-text">鹏仔主播音浪管理系统</h1>
        </div>
      </div>

      <div className="flex items-center gap-4">
        {/* 筛选按钮（只在数据统计页面显示） */}
        {showFilters && (
          <>
            {/* 主播筛选按钮 */}
            <div className="relative">
              <button
                onClick={() => setShowAnchorSelector(!showAnchorSelector)}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors flex items-center gap-2 ${
                  selectedAnchorIds.length > 0
                    ? 'bg-primary text-white'
                    : 'bg-slate-700 text-text-muted hover:text-text'
                }`}
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
                </svg>
                {selectedAnchorIds.length === 0 ? '全部主播' : `已选 ${selectedAnchorIds.length} 人`}
              </button>
              
              {showAnchorSelector && (
                <div className="absolute top-full right-0 mt-2 w-72 bg-background-light border border-slate-700 rounded-xl shadow-xl z-20">
                  <div className="p-3 border-b border-slate-700">
                    <input
                      type="text"
                      placeholder="搜索主播..."
                      value={anchorSearchTerm}
                      onChange={(e) => setAnchorSearchTerm(e.target.value)}
                      className="w-full px-3 py-2 bg-background border border-slate-600 rounded-lg text-text text-sm focus:outline-none focus:border-primary"
                    />
                  </div>
                  <div className="flex border-b border-slate-700">
                    <button
                      onClick={selectAllAnchors}
                      className="flex-1 px-3 py-2 text-sm text-primary hover:bg-slate-700 transition-colors"
                    >
                      全选
                    </button>
                    <button
                      onClick={clearAnchorSelection}
                      className="flex-1 px-3 py-2 text-sm text-text-muted hover:bg-slate-700 transition-colors"
                    >
                      清空
                    </button>
                  </div>
                  <div className="max-h-60 overflow-y-auto">
                    {filteredAnchors.map(anchorId => {
                      const anchorName = anchorMap.get(anchorId) || anchorId
                      const isSelected = selectedAnchorIds.includes(anchorId)
                      return (
                        <div
                          key={anchorId}
                          onClick={() => toggleAnchor(anchorId)}
                          className={`px-3 py-2 cursor-pointer flex items-center gap-2 hover:bg-slate-700 transition-colors ${
                            isSelected ? 'bg-primary/10' : ''
                          }`}
                        >
                          <div className={`w-4 h-4 rounded border flex items-center justify-center ${
                            isSelected ? 'bg-primary border-primary' : 'border-slate-500'
                          }`}>
                            {isSelected && (
                              <svg className="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                              </svg>
                            )}
                          </div>
                          <span className="text-sm text-text truncate">{anchorName}</span>
                        </div>
                      )
                    })}
                    {filteredAnchors.length === 0 && (
                      <div className="px-3 py-4 text-center text-text-muted text-sm">
                        未找到匹配的主播
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* 时间范围筛选 */}
            <div className="flex items-center gap-2">
              {(['day', 'week', 'month', 'custom'] as TimeRange[]).map((range) => (
                <button
                  key={range}
                  onClick={() => onTimeRangeChange(range)}
                  className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                    timeRange === range
                      ? 'bg-primary text-white'
                      : 'bg-slate-700 text-text-muted hover:text-text'
                  }`}
                >
                  {range === 'day' ? '今日' : range === 'week' ? '本周' : range === 'month' ? '本月' : '自定义'}
                </button>
              ))}
            </div>

            {timeRange === 'custom' && (
              <div className="flex items-center gap-2">
                <input
                  type="date"
                  value={customDateRange.start}
                  onChange={(e) => onCustomDateChange({ ...customDateRange, start: e.target.value })}
                  className="px-3 py-1.5 bg-background-light border border-slate-700 rounded-lg text-text text-sm"
                />
                <span className="text-text-muted">至</span>
                <input
                  type="date"
                  value={customDateRange.end}
                  onChange={(e) => onCustomDateChange({ ...customDateRange, end: e.target.value })}
                  className="px-3 py-1.5 bg-background-light border border-slate-700 rounded-lg text-text text-sm"
                />
              </div>
            )}
          </>
        )}

        {/* 主播列表搜索和添加按钮（只在主播列表页面显示） */}
        {showAnchorListSearch && (
          <>
            <div className="relative">
              <input
                type="text"
                placeholder="搜索主播ID或姓名..."
                value={anchorListSearchTerm}
                onChange={(e) => onAnchorListSearchChange(e.target.value)}
                className="w-64 px-4 py-2 pl-10 bg-background border border-slate-700 rounded-lg text-text placeholder-text-muted focus:outline-none focus:border-primary"
              />
              <svg className="w-5 h-5 text-text-muted absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <button
              onClick={onImportAnchors}
              className="px-4 py-2 bg-green-600 hover:bg-green-500 rounded-lg text-white text-sm font-medium transition-all duration-200 flex items-center gap-2 mr-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
              </svg>
              从文件导入
            </button>
            <button
              onClick={onAddAnchor}
              className="px-4 py-2 bg-primary hover:bg-primary-light rounded-lg text-white text-sm font-medium transition-all duration-200 flex items-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
              </svg>
              添加主播
            </button>
          </>
        )}

        {/* 数据中心年月选择（只在数据中心页面显示） */}
        {showDataCenterYearMonth && (
          <div className="flex items-center gap-2">
            <select
              value={selectedYearMonth}
              onChange={(e) => onYearMonthChange(e.target.value)}
              className="px-3 py-1.5 bg-slate-700 text-text border border-slate-600 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="">全部数据</option>
              {availableYearMonths.map(ym => (
                <option key={ym} value={ym}>
                  {ym.replace('-', '年')}月
                </option>
              ))}
            </select>
          </div>
        )}

        <button
          onClick={onSyncAnchors}
          disabled={loading || isSyncingData}
          className="px-4 py-2 bg-primary hover:bg-primary-light disabled:opacity-50 disabled:cursor-not-allowed rounded-lg text-white text-sm font-medium transition-all duration-200 flex items-center gap-2"
        >
          <svg className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          {loading ? '刷新中...' : isSyncingData ? '同步中...' : '刷新'}
        </button>

        {/* 最小化按钮 */}
        <button
          onClick={() => window.electronAPI.windowMinimize()}
          className="p-2 bg-slate-700 hover:bg-slate-600 rounded-lg text-text-muted hover:text-text transition-colors"
          title="最小化"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 12H4" />
          </svg>
        </button>

        {/* 全屏按钮 */}
        <button
          onClick={handleMaximize}
          className="p-2 bg-slate-700 hover:bg-slate-600 rounded-lg text-text-muted hover:text-text transition-colors"
          title={isMaximized ? '还原' : '全屏'}
        >
          {isMaximized ? (
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 4H6a2 2 0 00-2 2v2m0 8v2a2 2 0 002 2h2m8-16h2a2 2 0 012 2v2m0 8v2a2 2 0 01-2 2h-2" />
            </svg>
          ) : (
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V6a2 2 0 012-2h2M4 16v2a2 2 0 002 2h2m8-16h2a2 2 0 012 2v2m0 8v2a2 2 0 01-2 2h-2" />
            </svg>
          )}
        </button>

        {/* 关闭按钮 */}
        <button
          onClick={handleClose}
          className="p-2 bg-slate-700 hover:bg-red-600 rounded-lg text-text-muted hover:text-white transition-colors group"
          title="关闭"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </header>
  )
}

export default Header
