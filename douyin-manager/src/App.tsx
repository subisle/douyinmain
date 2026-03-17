import { useState, useEffect, useCallback } from 'react'
import { Anchor, WaveStat, DurationStat, TimeRange } from './types'
import { useToast } from './hooks/useToast'
import Header from './components/Header'
import Sidebar from './components/Sidebar'
import Dashboard from './components/Dashboard'
import DataCenter from './components/DataCenter'
import AnchorList from './components/AnchorList'
import StatsCharts from './components/StatsCharts'
import ImportModal from './components/ImportModal'
import ExportModal from './components/ExportModal'
import AnchorEditModal from './components/AnchorEditModal'
import AnchorImportModal from './components/AnchorImportModal'
import Toast from './components/Toast'

type Page = 'dashboard' | 'datacenter' | 'anchors' | 'stats'

function App() {
  const [currentPage, setCurrentPage] = useState<Page>('dashboard')
  const [anchors, setAnchors] = useState<Anchor[]>([])
  const [waveStats, setWaveStats] = useState<WaveStat[]>([])
  const [durationStats, setDurationStats] = useState<DurationStat[]>([])
  // 今日数据（独立于时间范围筛选）
  const [todayWaveStats, setTodayWaveStats] = useState<WaveStat[]>([])
  const [todayDurationStats, setTodayDurationStats] = useState<DurationStat[]>([])
  // 全部音浪数据（用于累计计算）
  const [allWaveStats, setAllWaveStats] = useState<WaveStat[]>([])
  const [allDurationStats, setAllDurationStats] = useState<DurationStat[]>([])
  
  const [loading, setLoading] = useState(false)
  const [showImportModal, setShowImportModal] = useState(false)
  const [showExportModal, setShowExportModal] = useState(false)
  const [showAnchorModal, setShowAnchorModal] = useState(false)
  const [showAnchorImportModal, setShowAnchorImportModal] = useState(false)
  const [editingAnchor, setEditingAnchor] = useState<Anchor | null>(null)
  const [importType, setImportType] = useState<'wave' | 'duration'>('wave')
  const [exportType, setExportType] = useState<'wave' | 'duration'>('wave')
  const [dropFilePath, setDropFilePath] = useState<string>('')
  const [timeRange, setTimeRange] = useState<TimeRange>('week')
  const [customDateRange, setCustomDateRange] = useState<{ start: string; end: string }>({
    start: '',
    end: ''
  })
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [selectedAnchorIds, setSelectedAnchorIds] = useState<string[]>([])
  const [anchorSearchTerm, setAnchorSearchTerm] = useState('')
  const [selectedYearMonth, setSelectedYearMonth] = useState<string>('')
  const [isSyncingData, setIsSyncingData] = useState(false) // 是否正在同步数据
  
  // 权限验证状态
  const [isAuthorized, setIsAuthorized] = useState<boolean | null>(null)

  // 权限验证
  useEffect(() => {
    const checkAuth = async () => {
      try {
        const result = await window.electronAPI.checkAuthorization()
        setIsAuthorized(result.authorized)
      } catch {
        setIsAuthorized(false)
      }
    }
    checkAuth()
  }, [])

  // 统一错误处理
  const { toast, showSuccess, showError, showInfo, hideToast, withErrorHandling } = useToast()

  const loadAnchors = useCallback(withErrorHandling(async () => {
    const result = await window.electronAPI.getAnchors()
    if (result.success && result.data) {
      setAnchors(result.data)
    } else {
      showError('加载主播列表失败')
    }
  }, '加载主播列表失败'), [withErrorHandling, showError])

  const syncAnchors = useCallback(withErrorHandling(async () => {
    setLoading(true)
    try {
      const result = await window.electronAPI.syncAnchorOnly()
      if (result.success) {
        await loadAnchors()
        showSuccess('主播同步成功')
      } else {
        showError('同步失败: ' + (result.error || '未知错误'))
      }
    } finally {
      setLoading(false)
    }
  }, '同步主播失败'), [withErrorHandling, loadAnchors, showSuccess, showError])

  // 加载今日数据（用于 Dashboard）
  const loadTodayStats = useCallback(async () => {
    try {
      const today = new Date().toISOString().split('T')[0]
      const filters = {
        startDate: today,
        endDate: today
      }

      const [waveResult, durationResult] = await Promise.all([
        window.electronAPI.getWaveStats(filters),
        window.electronAPI.getDurationStats(filters)
      ])

      if (waveResult.success && waveResult.data) {
        setTodayWaveStats(waveResult.data)
      }
      if (durationResult.success && durationResult.data) {
        setTodayDurationStats(durationResult.data)
      }
    } catch (err) {
      console.error('Failed to load today stats:', err)
    }
  }, [])

  // 加载全部数据（用于累计计算）
  const loadAllStats = useCallback(async () => {
    try {
      const [waveResult, durationResult] = await Promise.all([
        window.electronAPI.getWaveStats({}),
        window.electronAPI.getDurationStats({})
      ])

      if (waveResult.success && waveResult.data) {
        setAllWaveStats(waveResult.data)
      }
      if (durationResult.success && durationResult.data) {
        setAllDurationStats(durationResult.data)
      }
    } catch (err) {
      console.error('Failed to load all stats:', err)
    }
  }, [])

  // 加载统计数据（用于 StatsCharts 和 AnchorList）
  const loadStats = useCallback(async () => {
    try {
      const filters: any = {}

      if (customDateRange.start && customDateRange.end) {
        filters.startDate = customDateRange.start
        filters.endDate = customDateRange.end
      } else {
        const now = new Date()
        const days = timeRange === 'day' ? 1 : timeRange === 'week' ? 7 : timeRange === 'month' ? 30 : 7
        const startDate = new Date(now.getTime() - days * 24 * 60 * 60 * 1000)
        filters.startDate = startDate.toISOString().split('T')[0]
        filters.endDate = now.toISOString().split('T')[0]
      }

      const [waveResult, durationResult] = await Promise.all([
        window.electronAPI.getWaveStats(filters),
        window.electronAPI.getDurationStats(filters)
      ])

      if (waveResult.success && waveResult.data) {
        setWaveStats(waveResult.data)
      }
      if (durationResult.success && durationResult.data) {
        setDurationStats(durationResult.data)
      }
    } catch (err) {
      console.error('Failed to load stats:', err)
    }
  }, [timeRange, customDateRange])

  useEffect(() => {
    loadAnchors()
    loadTodayStats()
    loadStats()
    loadAllStats()
    
    // 程序启动时自动同步主播数据
    const autoSyncAnchors = async () => {
      try {
        const result = await window.electronAPI.syncAnchorOnly()
        if (result.success) {
          await loadAnchors() // 重新加载主播列表以反映同步结果
        }
      } catch (error) {
        console.error('Auto sync anchors failed:', error)
      }
    }
    
    autoSyncAnchors()
  }, []) // 空依赖，只加载一次

  const handleImport = (type: 'wave' | 'duration') => {
    setImportType(type)
    setShowImportModal(true)
  }

  // 拖拽导入处理
  const handleDropImport = (type: 'wave' | 'duration', filePath: string) => {
    setImportType(type)
    setDropFilePath(filePath)
    setShowImportModal(true)
  }

  const handleExport = (type: 'wave' | 'duration') => {
    setExportType(type)
    setShowExportModal(true)
  }

  const handleTimeRangeChange = (range: TimeRange) => {
    setTimeRange(range)
    if (range !== 'custom') {
      setCustomDateRange({ start: '', end: '' })
    }
  }

  // 主播管理
  const handleAddAnchor = () => {
    setEditingAnchor(null)
    setShowAnchorModal(true)
  }

  const handleImportAnchors = () => {
    setShowAnchorImportModal(true)
  }

  const handleEditAnchor = (anchor: Anchor) => {
    setEditingAnchor(anchor)
    setShowAnchorModal(true)
  }

  const handleDeleteAnchor = async (anchor: Anchor) => {
    if (!confirm(`确定要删除主播 "${anchor.anchor_name}" 吗？\n此操作将同时删除远程数据库中的数据。`)) {
      return
    }
    
    try {
      const result = await window.electronAPI.deleteAnchor(anchor.id)
      if (result.success) {
        await loadAnchors()
      } else {
        alert('删除失败: ' + result.error)
      }
    } catch (err) {
      console.error('Delete error:', err)
    }
  }

  // 单个主播导入功能
  const handleImportSingle = async (anchor: Anchor) => {
    // 这里可以打开特定的导入模态框或执行导入逻辑
    // 暂时显示一个提示
    alert(`准备导入主播: ${anchor.anchor_name || anchor.anchor_id}\n您可以在此处添加具体的导入逻辑`);
  }

  const handleAnchorModalSuccess = async () => {
    setShowAnchorModal(false)
    setEditingAnchor(null)
    await loadAnchors()
  }

  // 导入成功后刷新所有数据
  const handleImportSuccess = async () => {
    setShowImportModal(false)
    await Promise.all([loadTodayStats(), loadStats(), loadAllStats()])
  }

  // 清除数据后刷新
  const handleDataCleared = async () => {
    // 先重置状态为空数组，防止旧数据残留
    setAllWaveStats([])
    setAllDurationStats([])
    setTodayWaveStats([])
    setTodayDurationStats([])
    setWaveStats([])
    setDurationStats([])
    // 然后重新加载数据
    await Promise.all([loadTodayStats(), loadStats(), loadAllStats()])
  }

  // 权限验证中
  if (isAuthorized === null) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-text-muted">系统启动中...</p>
        </div>
      </div>
    )
  }

  return (
    <div className={`min-h-screen bg-background ${!isAuthorized ? 'garbled-mode select-none' : ''}`}>
      <Header
        onSyncAnchors={syncAnchors}
        loading={loading}
        isSyncingData={isSyncingData}
        currentPage={currentPage}
        timeRange={timeRange}
        customDateRange={customDateRange}
        onTimeRangeChange={handleTimeRangeChange}
        onCustomDateChange={setCustomDateRange}
        anchors={anchors}
      waveStats={waveStats.map(stat => ({
        ...stat,
        date: typeof stat.date === 'string' ? stat.date : stat.date.toISOString().split('T')[0]
      }))}
      durationStats={durationStats.map(stat => ({
        ...stat,
        date: typeof stat.date === 'string' ? stat.date : stat.date.toISOString().split('T')[0]
      }))}
        selectedAnchorIds={selectedAnchorIds}
        onAnchorSelectionChange={setSelectedAnchorIds}
        anchorListSearchTerm={anchorSearchTerm}
        onAnchorListSearchChange={setAnchorSearchTerm}
        onAddAnchor={handleAddAnchor}
        onImportAnchors={handleImportAnchors}
        selectedYearMonth={selectedYearMonth}
        onYearMonthChange={setSelectedYearMonth}
        onExport={handleExport}
      />

      <div className="flex">
        <Sidebar
          currentPage={currentPage}
          onPageChange={setCurrentPage}
          onCollapsedChange={setSidebarCollapsed}
          onDataCleared={handleDataCleared}
        />

        <main className={`flex-1 p-6 pt-20 transition-all duration-300 ${sidebarCollapsed ? 'ml-16' : 'ml-64'}`}>
          {currentPage === 'dashboard' && (
            <Dashboard
              anchors={anchors}
              todayWaveStats={todayWaveStats}
              todayDurationStats={todayDurationStats}
              allWaveStats={allWaveStats}
              allDurationStats={allDurationStats}
              onImport={handleImport}
              onExport={handleExport}
              onDropImport={handleDropImport}
            />
          )}

          {currentPage === 'datacenter' && (
            <DataCenter
              anchors={anchors}
              waveStats={waveStats}
              durationStats={durationStats}
              selectedYearMonth={selectedYearMonth}
              totalAnchors={anchors.length}
            />
          )}

          {currentPage === 'anchors' && (
            <AnchorList
              anchors={anchors}
              waveStats={waveStats}
              durationStats={durationStats}
              onEdit={handleEditAnchor}
              onDelete={handleDeleteAnchor}
              onImportSingle={handleImportSingle}
              searchTerm={anchorSearchTerm}
            />
          )}

          {currentPage === 'stats' && (
            <StatsCharts
              waveStats={waveStats}
              durationStats={durationStats}
              anchors={anchors}
              selectedAnchorIds={selectedAnchorIds}
            />
          )}
        </main>
      </div>

      {showImportModal && (
        <ImportModal
          type={importType}
          anchors={anchors}
          dropFilePath={dropFilePath}
          onClose={() => {
            setShowImportModal(false)
            setDropFilePath('')
          }}
          onSuccess={handleImportSuccess}
          onImportStart={() => setIsSyncingData(true)}
          onImportComplete={() => setIsSyncingData(false)}
        />
      )}

      {showExportModal && (
        <ExportModal
          type={exportType}
          waveStats={waveStats}
          durationStats={durationStats}
          anchors={anchors}
          onClose={() => setShowExportModal(false)}
        />
      )}

      {showAnchorModal && (
        <AnchorEditModal
          anchor={editingAnchor}
          onClose={() => {
            setShowAnchorModal(false)
            setEditingAnchor(null)
          }}
          onSuccess={handleAnchorModalSuccess}
        />
      )}

      {showAnchorImportModal && (
        <AnchorImportModal
          anchors={anchors}
          onClose={() => setShowAnchorImportModal(false)}
          onSuccess={() => {
            setShowAnchorImportModal(false)
            loadAnchors() // 重新加载主播列表
          }}
        />
      )}
    </div>
  )
}

export default App
