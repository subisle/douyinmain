import { useState, useEffect, useMemo } from 'react'
import { Anchor, WaveStat, DurationStat } from '../types'

interface AnchorListProps {
  anchors: Anchor[]
  waveStats: WaveStat[]
  durationStats: DurationStat[]
  onEdit: (anchor: Anchor) => void
  onDelete: (anchor: Anchor) => void
  searchTerm: string
}

type GenderFilter = 'all' | 'male' | 'female'

// 格式化日期（处理 Date 对象或字符串）
const formatDateStr = (date: Date | string | undefined): string => {
  if (!date) return ''
  if (typeof date === 'string') return date
  if (date instanceof Date) {
    return date.toISOString().split('T')[0]
  }
  // 如果是其他类型，尝试转换为字符串
  return String(date)
}

function AnchorList({ anchors, waveStats, durationStats, onEdit, onDelete, searchTerm }: AnchorListProps) {
  const [showSyncHint, setShowSyncHint] = useState(false)
  const [genderFilter, setGenderFilter] = useState<GenderFilter>('all')

  // 检测是否有统计数据但没有主播数据，提示同步
  useEffect(() => {
    if (anchors.length === 0 && (waveStats.length > 0 || durationStats.length > 0)) {
      setShowSyncHint(true)
    } else {
      setShowSyncHint(false)
    }
  }, [anchors.length, waveStats.length, durationStats.length])

  // 使用 useMemo 优化性能，避免重复计算
  const latestStatsMap = useMemo(() => {
    const map = new Map<string, { wave: WaveStat | null; duration: DurationStat | null }>()
    
    // 预处理：按anchor_id分组统计
    const waveStatsByAnchor = new Map<string, WaveStat[]>()
    const durationStatsByAnchor = new Map<string, DurationStat[]>()

    // 按anchor_id分组
    waveStats.forEach(stat => {
      if (!waveStatsByAnchor.has(stat.anchor_id)) {
        waveStatsByAnchor.set(stat.anchor_id, [])
      }
      waveStatsByAnchor.get(stat.anchor_id)!.push(stat)
    })

    durationStats.forEach(stat => {
      if (!durationStatsByAnchor.has(stat.anchor_id)) {
        durationStatsByAnchor.set(stat.anchor_id, [])
      }
      durationStatsByAnchor.get(stat.anchor_id)!.push(stat)
    })

    // 计算每个主播的最新统计数据
    anchors.forEach(anchor => {
      // 获取该主播的最新音浪数据
      const anchorWaveStats = waveStatsByAnchor.get(anchor.anchor_id) || []
      let latestWave: WaveStat | null = null
      if (anchorWaveStats.length > 0) {
        let maxDate = null
        for (const stat of anchorWaveStats) {
          const dateStr = formatDateStr(stat.date)
          if (dateStr) {
            const statDate = new Date(dateStr)
            if (statDate.toString() !== 'Invalid Date' && (!maxDate || statDate > maxDate)) {
              maxDate = statDate
              latestWave = stat
            }
          }
        }
      }

      // 获取该主播的最新时长数据
      const anchorDurationStats = durationStatsByAnchor.get(anchor.anchor_id) || []
      let latestDuration: DurationStat | null = null
      if (anchorDurationStats.length > 0) {
        let maxDate = null
        for (const stat of anchorDurationStats) {
          const dateStr = formatDateStr(stat.date)
          if (dateStr) {
            const statDate = new Date(dateStr)
            if (statDate.toString() !== 'Invalid Date' && (!maxDate || statDate > maxDate)) {
              maxDate = statDate
              latestDuration = stat
            }
          }
        }
      }

      map.set(anchor.anchor_id, { wave: latestWave, duration: latestDuration })
    })
    
    return map
  }, [anchors, waveStats, durationStats])

  // 按主播ID或主播姓名和性别筛选
  const filteredAnchors = useMemo(() => {
    return anchors.filter(anchor =>
      (anchor.anchor_id.includes(searchTerm) ||
      (anchor.anchor_name && anchor.anchor_name.includes(searchTerm))) &&
      (genderFilter === 'all' || anchor.gender === genderFilter)
    )
  }, [anchors, searchTerm, genderFilter])

  const getLatestWave = (anchorId: string) => {
    return latestStatsMap.get(anchorId)?.wave || null
  }

  const getLatestDuration = (anchorId: string) => {
    return latestStatsMap.get(anchorId)?.duration || null
  }

  const formatDuration = (minutes: number) => {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return hours > 0 ? `${hours}小时${mins}分` : `${mins}分钟`
  }

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

  const getGenderLabel = (gender?: string) => {
    if (gender === 'female') return '女'
    return '男'
  }

  return (
    <div className="space-y-6">
      {/* 同步提示 */}
      {showSyncHint && (
        <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-xl p-4 flex items-center gap-3">
          <svg className="w-6 h-6 text-yellow-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div className="flex-1">
            <p className="text-yellow-400 font-medium">检测到有统计数据但无主播信息</p>
            <p className="text-yellow-400/70 text-sm">请点击右上角"同步主播"按钮从远程数据库同步主播信息</p>
          </div>
        </div>
      )}

      <div className="bg-background-light rounded-xl border border-slate-700 overflow-hidden">
        <div className="p-4 border-b border-slate-700 flex items-center gap-4">
          <div className="flex items-center gap-2">
            <span className="text-text-muted text-sm">性别筛选:</span>
            <button
              onClick={() => setGenderFilter('all')}
              className={`px-3 py-1 rounded-lg text-sm font-medium transition-colors ${
                genderFilter === 'all'
                  ? 'bg-primary text-white'
                  : 'bg-slate-700 text-text-muted hover:text-text'
              }`}
            >
              全部
            </button>
            <button
              onClick={() => setGenderFilter('male')}
              className={`px-3 py-1 rounded-lg text-sm font-medium transition-colors ${
                genderFilter === 'male'
                  ? 'bg-blue-500 text-white'
                  : 'bg-slate-700 text-text-muted hover:text-text'
              }`}
            >
              男
            </button>
            <button
              onClick={() => setGenderFilter('female')}
              className={`px-3 py-1 rounded-lg text-sm font-medium transition-colors ${
                genderFilter === 'female'
                  ? 'bg-pink-500 text-white'
                  : 'bg-slate-700 text-text-muted hover:text-text'
              }`}
            >
              女
            </button>
          </div>
        </div>
        <div className="overflow-auto max-h-[70vh]">
          <table className="w-full">
            <thead className="bg-slate-800/50 sticky top-0 z-10">
              <tr className="text-left text-text-muted text-sm">
                <th className="px-6 py-4 font-medium whitespace-nowrap">序号</th>
                <th className="px-6 py-4 font-medium whitespace-nowrap">主播ID</th>
                <th className="px-6 py-4 font-medium whitespace-nowrap">主播姓名</th>
                <th className="px-6 py-4 font-medium whitespace-nowrap">性别</th>
                <th className="px-6 py-4 font-medium whitespace-nowrap">最新音浪</th>
                <th className="px-6 py-4 font-medium whitespace-nowrap">最新时长</th>
                <th className="px-6 py-4 font-medium whitespace-nowrap">数据日期</th>
                <th className="px-6 py-4 font-medium whitespace-nowrap">操作</th>
              </tr>
            </thead>
            <tbody>
              {filteredAnchors.map((anchor, index) => {
                const latestWave = getLatestWave(anchor.anchor_id)
                const latestDuration = getLatestDuration(anchor.anchor_id)
                // 使用连续序号，而不是数据库中的 serial_number
                const displayIndex = index + 1

                return (
                  <tr key={`${anchor.id}-${anchor.anchor_id}`} className="border-t border-slate-700/50 hover:bg-slate-800/30 transition-colors">
                    <td className="px-6 py-4 text-text-muted font-mono text-sm whitespace-nowrap">{displayIndex}</td>
                    <td className="px-6 py-4 text-text-muted font-mono text-sm whitespace-nowrap">{anchor.anchor_id}</td>
                    <td className="px-6 py-4 text-text font-medium whitespace-nowrap">{anchor.anchor_name || '-'}</td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-2 py-1 rounded text-xs ${anchor.gender === 'female' ? 'bg-pink-500/20 text-pink-400' : 'bg-blue-500/20 text-blue-400'}`}>
                        {getGenderLabel(anchor.gender)}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-accent-gold font-mono whitespace-nowrap">
                      {latestWave ? formatWaveValue(latestWave.wave_value) : '-'}
                    </td>
                    <td className="px-6 py-4 text-accent-blue whitespace-nowrap">
                      {latestDuration ? formatDuration(latestDuration.duration_minutes) : '-'}
                    </td>
                    <td className="px-6 py-4 text-text-muted text-sm whitespace-nowrap">
                      {formatDateStr(latestWave?.date || latestDuration?.date || '') || '-'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => onEdit(anchor)}
                          className="p-1.5 hover:bg-slate-700 rounded transition-colors text-text-muted hover:text-primary"
                          title="编辑"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path str