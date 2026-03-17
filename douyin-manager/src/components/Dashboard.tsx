import { useState, useCallback } from 'react'
import { Anchor, WaveStat, DurationStat } from '../types'
import DataTableImageExport from './DataTableImageExport'
import DurationTableImageExport from './DurationTableImageExport'

interface DashboardProps {
  anchors: Anchor[]
  todayWaveStats: WaveStat[]
  todayDurationStats: DurationStat[]
  allWaveStats: WaveStat[]
  allDurationStats: DurationStat[]
  onImport: (type: 'wave' | 'duration') => void
  onExport: (type: 'wave' | 'duration') => void
  onDropImport?: (type: 'wave' | 'duration', filePath: string) => void
}

function Dashboard({ anchors, todayWaveStats, todayDurationStats, allWaveStats, allDurationStats, onImport, onExport, onDropImport }: DashboardProps) {
  const [showImageExport, setShowImageExport] = useState(false)
  const [showDurationImageExport, setShowDurationImageExport] = useState(false)
  const [waveDragOver, setWaveDragOver] = useState(false)
  const [durationDragOver, setDurationDragOver] = useState(false)

  const totalAnchors = anchors.length

  // 确保 todayWaveStats 和 todayDurationStats 是数组
  const safeTodayWaveStats = Array.isArray(todayWaveStats) ? todayWaveStats : []
  const safeTodayDurationStats = Array.isArray(todayDurationStats) ? todayDurationStats : []
  const safeAllWaveStats = Array.isArray(allWaveStats) ? allWaveStats : []
  const safeAllDurationStats = Array.isArray(allDurationStats) ? allDurationStats : []

  // 计算每个主播的累计音浪
  const anchorTotalWaves = safeAllWaveStats.reduce((acc, stat) => {
    const anchorId = stat.anchor_id
    acc.set(anchorId, (acc.get(anchorId) || 0) + stat.wave_value)
    return acc
  }, new Map<string, number>())

  // 总音浪累计
  const totalWaveAllTime = Array.from(anchorTotalWaves.values()).reduce((sum, v) => sum + v, 0)

  // 按音浪值重新排序计算正确的排名
  const sortedTodayWaveStats = safeTodayWaveStats
    .sort((a, b) => b.wave_value - a.wave_value)
    .map((stat, index) => ({ ...stat, rank: index + 1 }))

  // 使用传入的今日数据
  const todayTotalWaves = safeTodayWaveStats.reduce((sum, s) => sum + s.wave_value, 0)

  const todayTotalMinutes = safeTodayDurationStats.reduce((sum, s) => sum + s.duration_minutes, 0)
  const todayHours = Math.floor(todayTotalMinutes / 60)
  const todayMins = todayTotalMinutes % 60

  // 格式化音浪数值：800022 -> 80万22, 223444 -> 22万3444
  const formatWaveValue = (num: number) => {
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

  const formatNumber = (num: number) => {
    return formatWaveValue(num)
  }

  // 格式化日期（处理 Date 对象或字符串）
  const formatDate = (date: Date | string): string => {
    if (!date) return '-'
    if (typeof date === 'string') return date
    return date.toISOString().split('T')[0]
  }

  // 拖拽处理 - 音浪数据区域
  const handleWaveDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setWaveDragOver(true)
  }, [])

  const handleWaveDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setWaveDragOver(false)
  }, [])

  const handleWaveDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setWaveDragOver(false)
    
    const files = e.dataTransfer.files
    if (files.length > 0 && onDropImport) {
      const file = files[0]
      if (file.path) {
        onDropImport('wave', file.path)
      }
    }
  }, [onDropImport])

  // 拖拽处理 - 时长数据区域
  const handleDurationDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setDurationDragOver(true)
  }, [])

  const handleDurationDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setDurationDragOver(false)
  }, [])

  const handleDurationDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setDurationDragOver(false)
    
    const files = e.dataTransfer.files
    if (files.length > 0 && onDropImport) {
      const file = files[0]
      if (file.path) {
        onDropImport('duration', file.path)
      }
    }
  }, [onDropImport])

  return (
    <div className="space-y-6">
      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6">
        <div className="bg-background-light rounded-xl p-6 border border-slate-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-text-muted text-sm">主播总数</p>
              <p className="text-3xl font-bold text-text mt-2">{totalAnchors}</p>
            </div>
            <div className="w-12 h-12 rounded-lg bg-primary/20 flex items-center justify-center">
              <svg className="w-6 h-6 text-primary-light" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </div>
          </div>
        </div>

        <div className="bg-background-light rounded-xl p-6 border border-slate-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-text-muted text-sm">今日音浪</p>
              <p className="text-3xl font-bold text-accent-gold mt-2">{formatNumber(todayTotalWaves)}</p>
            </div>
            <div className="w-12 h-12 rounded-lg bg-accent-gold/20 flex items-center justify-center">
              <svg className="w-6 h-6 text-accent-gold" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </div>

        <div className="bg-background-light rounded-xl p-6 border border-slate-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-text-muted text-sm">累计总音浪</p>
              <p className="text-3xl font-bold text-orange-500 mt-2">{formatNumber(totalWaveAllTime)}</p>
            </div>
            <div className="w-12 h-12 rounded-lg bg-orange-500/20 flex items-center justify-center">
              <svg className="w-6 h-6 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
            </div>
          </div>
        </div>

        <div className="bg-background-light rounded-xl p-6 border border-slate-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-text-muted text-sm">今日直播时长</p>
              <p className="text-3xl font-bold text-accent-blue mt-2">{todayHours}小时{todayMins}分</p>
            </div>
            <div className="w-12 h-12 rounded-lg bg-accent-blue/20 flex items-center justify-center">
              <svg className="w-6 h-6 text-accent-blue" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </div>

        <div className="bg-background-light rounded-xl p-6 border border-slate-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-text-muted text-sm">今日开播人数</p>
              <p className="text-3xl font-bold text-accent-green mt-2">{safeTodayWaveStats.length}</p>
            </div>
            <div className="w-12 h-12 rounded-lg bg-accent-green/20 flex items-center justify-center">
              <svg className="w-6 h-6 text-accent-green" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div 
          className={`bg-background-light rounded-xl p-6 border-2 border-dashed transition-all ${
            waveDragOver 
              ? 'border-accent-gold bg-accent-gold/10 scale-[1.02]' 
              : 'border-slate-700'
          }`}
          onDragOver={handleWaveDragOver}
          onDragLeave={handleWaveDragLeave}
          onDrop={handleWaveDrop}
        >
          <h3 className="text-lg font-semibold text-text mb-4">音浪数据</h3>
          
          {/* 拖拽提示 */}
          <div 
            className={`mb-4 p-4 rounded-lg border-2 border-dashed transition-all ${
              waveDragOver 
                ? 'border-accent-gold bg-accent-gold/20' 
                : 'border-slate-600 hover:border-accent-gold/50'
            }`}
          >
            <div className="flex flex-col items-center justify-center text-text-muted">
              <svg className={`w-8 h-8 mb-2 transition-colors ${waveDragOver ? 'text-accent-gold' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
              </svg>
              <span className={`text-sm ${waveDragOver ? 'text-accent-gold font-medium' : ''}`}>
                {waveDragOver ? '松开导入音浪数据' : '拖拽 CSV 文件到此处导入'}
              </span>
            </div>
          </div>

          <div className="space-y-3">
            <button
              onClick={() => onImport('wave')}
              className="w-full py-3 px-4 bg-accent-gold/20 hover:bg-accent-gold/30 text-accent-gold rounded-lg transition-colors flex items-center justify-center gap-2"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
              </svg>
              导入音浪CSV
            </button>
            <button
              onClick={() => setShowImageExport(true)}
              disabled={safeAllWaveStats.length === 0}
              className="w-full py-3 px-4 bg-slate-700 hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed text-text rounded-lg transition-colors flex items-center justify-center gap-2"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              导出数据
            </button>
          </div>
        </div>

        <div 
          className={`bg-background-light rounded-xl p-6 border-2 border-dashed transition-all ${
            durationDragOver 
              ? 'border-accent-blue bg-accent-blue/10 scale-[1.02]' 
              : 'border-slate-700'
          }`}
          onDragOver={handleDurationDragOver}
          onDragLeave={handleDurationDragLeave}
          onDrop={handleDurationDrop}
        >
          <h3 className="text-lg font-semibold text-text mb-4">时长数据</h3>
          
          {/* 拖拽提示 */}
          <div 
            className={`mb-4 p-4 rounded-lg border-2 border-dashed transition-all ${
              durationDragOver 
                ? 'border-accent-blue bg-accent-blue/20' 
                : 'border-slate-600 hover:border-accent-blue/50'
            }`}
          >
            <div className="flex flex-col items-center justify-center text-text-muted">
              <svg className={`w-8 h-8 mb-2 transition-colors ${durationDragOver ? 'text-accent-blue' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
              </svg>
              <span className={`text-sm ${durationDragOver ? 'text-accent-blue font-medium' : ''}`}>
                {durationDragOver ? '松开导入时长数据' : '拖拽 CSV 文件到此处导入'}
              </span>
            </div>
          </div>

          <div className="space-y-3">
            <button
              onClick={() => onImport('duration')}
              className="w-full py-3 px-4 bg-accent-blue/20 hover:bg-accent-blue/30 text-accent-blue rounded-lg transition-colors flex items-center justify-center gap-2"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
              </svg>
              导入时长CSV
            </button>
            <button
              onClick={() => setShowDurationImageExport(true)}
              disabled={safeAllDurationStats.length === 0}
              className="w-full py-3 px-4 bg-slate-700 hover:bg-slate-600 disabled:opacity-50 disabled:cursor-not-allowed text-text rounded-lg transition-colors flex items-center justify-center gap-2"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              导出数据
            </button>
          </div>
        </div>
      </div>

      {/* Recent Data */}
      <div className="bg-background-light rounded-xl p-6 border border-slate-700">
        <h3 className="text-lg font-semibold text-text mb-4">今日音浪排行 TOP 10</h3>
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="text-left text-text-muted text-sm border-b border-slate-700">
                <th className="pb-3 font-medium">排名</th>
                <th className="pb-3 font-medium">主播</th>
                <th className="pb-3 font-medium">音浪</th>
                <th className="pb-3 font-medium">日期</th>
              </tr>
            </thead>
            <tbody>
              {sortedTodayWaveStats.slice(0, 10).map((stat, index) => (
                <tr key={stat.id} className="border-b border-slate-700/50">
                  <td className="py-3">
                    <span className={`inline-flex items-center justify-center w-6 h-6 rounded-full text-xs font-medium ${
                      index < 3 ? 'bg-accent-gold/20 text-accent-gold' : 'bg-slate-700 text-text-muted'
                    }`}>
                      {stat.rank}
                    </span>
                  </td>
                  <td className="py-3 text-text">{stat.anchor_name || stat.anchor_id}</td>
                  <td className="py-3 text-accent-gold font-mono">{formatWaveValue(stat.wave_value)}</td>
                  <td className="py-3 text-text-muted">{formatDate(stat.date)}</td>
                </tr>
              ))}
              {sortedTodayWaveStats.length === 0 && (
                <tr>
                  <td colSpan={4} className="py-8 text-center text-text-muted">
                    暂无今日数据，请导入CSV文件
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* 数据表格图片导出弹窗 */}
      {showImageExport && (
        <DataTableImageExport
          anchors={anchors}
          date={new Date().toISOString().split('T')[0]}
          allWaveStats={safeAllWaveStats}
          allDurationStats={safeAllDurationStats}
          onClose={() => setShowImageExport(false)}
        />
      )}

      {/* 时长表格图片导出弹窗 */}
      {showDurationImageExport && (
        <DurationTableImageExport
          anchors={anchors}
          date={new Date().toISOString().split('T')[0]}
          allDurationStats={safeAllDurationStats}
          onClose={() => setShowDurationImageExport(false)}
        />
      )}
    </div>
  )
}

export default Dashboard
