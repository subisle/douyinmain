import { useState, useMemo } from 'react'
import { Anchor, WaveStat, DurationStat } from '../types'

interface DataCenterProps {
  anchors: Anchor[]
  waveStats: WaveStat[]
  durationStats: DurationStat[]
  selectedYearMonth: string
  totalAnchors?: number // 添加总主播数参数
}

// 默认等级阈值配置（单位：万）
const DEFAULT_GRADE_THRESHOLDS = {
  S: 300,   // 300万以上
  A: 150,   // 150万以上
  B: 80,    // 80万以上
  C: 30     // 30万以上，以下为D
}

// 根据阈值获取等级字母
const getGradeLetter = (waveValue: number, thresholds: typeof DEFAULT_GRADE_THRESHOLDS): string => {
  const wan = waveValue / 10000
  if (wan >= thresholds.S) return 'S'
  if (wan >= thresholds.A) return 'A'
  if (wan >= thresholds.B) return 'B'
  if (wan >= thresholds.C) return 'C'
  return 'D'
}

// 等级颜色配置
const GRADE_COLORS: Record<string, string> = {
  'S': 'bg-gradient-to-r from-yellow-500 to-orange-500 text-white',
  'A': 'bg-gradient-to-r from-purple-500 to-pink-500 text-white',
  'B': 'bg-gradient-to-r from-blue-500 to-cyan-500 text-white',
  'C': 'bg-gradient-to-r from-green-500 to-teal-500 text-white',
  'D': 'bg-slate-500 text-white'
}

// 格式化音浪数值：800022 -> 80万22
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

// 格式化时长
const formatDuration = (minutes: number) => {
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return hours > 0 ? `${hours}小时${mins}分` : `${mins}分钟`
}

// 格式化日期
const formatDateStr = (date: Date | string): string => {
  if (!date) return ''
  if (typeof date === 'string') return date
  return date.toISOString().split('T')[0]
}

type GenderFilter = 'all' | 'male' | 'female'
type SortOrder = 'desc' | 'asc'
type LevelFilter = 'all' | 'S' | 'A' | 'B' | 'C' | 'D'

function DataCenter({ anchors, waveStats, durationStats, selectedYearMonth, totalAnchors }: DataCenterProps) {
  const [genderFilter, setGenderFilter] = useState<GenderFilter>('male')
  const [levelFilter, setLevelFilter] = useState<LevelFilter>('all')
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc')
  const [activeTab, setActiveTab] = useState<'wave' | 'duration'>('wave')
  
  // 等级阈值配置
  const [gradeThresholds, setGradeThresholds] = useState(DEFAULT_GRADE_THRESHOLDS)

  // 根据年月过滤数据
  const filteredWaveStats = useMemo(() => {
    if (!selectedYearMonth) return waveStats
    return waveStats.filter(stat => {
      const date = formatDateStr(stat.date)
      if (!date) return false
      const [year, month] = date.split('-')
      return `${year}-${month}` === selectedYearMonth
    })
  }, [waveStats, selectedYearMonth])

  const filteredDurationStats = useMemo(() => {
    if (!selectedYearMonth) return durationStats
    return durationStats.filter(stat => {
      const date = formatDateStr(stat.date)
      if (!date) return false
      const [year, month] = date.split('-')
      return `${year}-${month}` === selectedYearMonth
    })
  }, [durationStats, selectedYearMonth])

  // 计算每个主播的统计数据
  const anchorStats = useMemo(() => {
    const statsMap = new Map<string, {
      anchor: Anchor
      totalWave: number
      totalDuration: number
      latestWave: WaveStat | null
      latestDuration: DurationStat | null
      waveCount: number
      durationCount: number
    }>()

    // 初始化主播数据
    anchors.forEach(anchor => {
      statsMap.set(anchor.anchor_id, {
        anchor,
        totalWave: 0,
        totalDuration: 0,
        latestWave: null,
        latestDuration: null,
        waveCount: 0,
        durationCount: 0
      })
    })

    // 汇总音浪数据（使用过滤后的数据）
    filteredWaveStats.forEach(stat => {
      let data = statsMap.get(stat.anchor_id)
      if (!data) {
        // 如果主播不在列表中，创建一个临时数据
        const tempAnchor: Anchor = {
          id: 0,
          anchor_id: stat.anchor_id,
          anchor_name: stat.anchor_name || stat.anchor_id,
          serial_number: 0,
          gender: 'male'
        }
        data = {
          anchor: tempAnchor,
          totalWave: 0,
          totalDuration: 0,
          latestWave: null,
          latestDuration: null,
          waveCount: 0,
          durationCount: 0
        }
        statsMap.set(stat.anchor_id, data)
      }
      data.totalWave += stat.wave_value
      data.waveCount++
      if (!data.latestWave || new Date(formatDateStr(stat.date)) > new Date(formatDateStr(data.latestWave.date))) {
        data.latestWave = stat
      }
    })

    // 汇总时长数据（使用过滤后的数据）
    filteredDurationStats.forEach(stat => {
      let data = statsMap.get(stat.anchor_id)
      if (!data) {
        const tempAnchor: Anchor = {
          id: 0,
          anchor_id: stat.anchor_id,
          anchor_name: stat.anchor_name || stat.anchor_id,
          serial_number: 0,
          gender: 'male'
        }
        data = {
          anchor: tempAnchor,
          totalWave: 0,
          totalDuration: 0,
          latestWave: null,
          latestDuration: null,
          waveCount: 0,
          durationCount: 0
        }
        statsMap.set(stat.anchor_id, data)
      }
      data.totalDuration += stat.duration_minutes
      data.durationCount++
      if (!data.latestDuration || new Date(formatDateStr(stat.date)) > new Date(formatDateStr(data.latestDuration.date))) {
        data.latestDuration = stat
      }
    })

    return Array.from(statsMap.values())
  }, [anchors, filteredWaveStats, filteredDurationStats])

  // 筛选和排序（添加等级序号）
  const filteredAndSortedStats = useMemo(() => {
    let result = anchorStats.filter(stat => {
      // 性别筛选
      if (genderFilter !== 'all' && stat.anchor.gender !== genderFilter) {
        return false
      }
      // 等级筛选
      if (levelFilter !== 'all') {
        const letter = getGradeLetter(stat.totalWave, gradeThresholds)
        if (letter !== levelFilter) {
          return false
        }
      }
      return true
    })

    // 排序 - 根据 activeTab 决定排序字段
    result.sort((a, b) => {
      const valueA = activeTab === 'wave' ? a.totalWave : a.totalDuration
      const valueB = activeTab === 'wave' ? b.totalWave : b.totalDuration
      if (sortOrder === 'desc') {
        return valueB - valueA
      }
      return valueA - valueB
    })

    // 分配等级序号
    const gradeCountMap = new Map<string, number>()
    return result.map(stat => {
      const letter = getGradeLetter(stat.totalWave, gradeThresholds)
      const count = gradeCountMap.get(letter) || 0
      gradeCountMap.set(letter, count + 1)
      return {
        ...stat,
        grade: `${letter}${count + 1}`,
        gradeLetter: letter
      }
    })
  }, [anchorStats, genderFilter, levelFilter, sortOrder, activeTab, gradeThresholds])

  // 统计数据
  const summary = useMemo(() => {
    const displayedAnchors = filteredAndSortedStats.length
    const totalAnchorsCount = totalAnchors ?? displayedAnchors // 使用传入的总数或显示的数量
    const totalWave = filteredAndSortedStats.reduce((sum, s) => sum + s.totalWave, 0)
    const totalDuration = filteredAndSortedStats.reduce((sum, s) => sum + s.totalDuration, 0)
    const avgWave = displayedAnchors > 0 ? Math.round(totalWave / displayedAnchors) : 0
    const avgDuration = displayedAnchors > 0 ? Math.round(totalDuration / displayedAnchors) : 0

    // 等级分布
    const levelDistribution = {
      S: filteredAndSortedStats.filter(s => s.gradeLetter === 'S').length,
      A: filteredAndSortedStats.filter(s => s.gradeLetter === 'A').length,
      B: filteredAndSortedStats.filter(s => s.gradeLetter === 'B').length,
      C: filteredAndSortedStats.filter(s => s.gradeLetter === 'C').length,
      D: filteredAndSortedStats.filter(s => s.gradeLetter === 'D').length,
    }

    return { totalAnchors: totalAnchorsCount, displayedAnchors, totalWave, totalDuration, avgWave, avgDuration, levelDistribution }
  }, [filteredAndSortedStats, totalAnchors])

  return (
    <div className="space-y-6">
      {/* 统计概览和等级分布 */}
      <div className="bg-background-light rounded-xl p-4 border border-slate-700">
        {/* 统计概览 */}
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3 mb-4">
          <div className="bg-slate-800/50 rounded-lg p-3">
            <p className="text-text-muted text-xs mb-1">主播总数</p>
            <p className="text-xl font-bold text-text">{summary.totalAnchors}</p>
          </div>
          <div className="bg-slate-800/50 rounded-lg p-3">
            <p className="text-text-muted text-xs mb-1">总音浪</p>
            <p className="text-xl font-bold text-accent-gold">{formatWaveValue(summary.totalWave)}</p>
          </div>
          <div className="bg-slate-800/50 rounded-lg p-3">
            <p className="text-text-muted text-xs mb-1">总时长</p>
            <p className="text-xl font-bold text-accent-blue">{formatDuration(summary.totalDuration)}</p>
          </div>
          <div className="bg-slate-800/50 rounded-lg p-3">
            <p className="text-text-muted text-xs mb-1">平均音浪</p>
            <p className="text-xl font-bold text-accent-green">{formatWaveValue(summary.avgWave)}</p>
          </div>
          <div className="bg-slate-800/50 rounded-lg p-3">
            <p className="text-text-muted text-xs mb-1">平均时长</p>
            <p className="text-xl font-bold text-accent-purple">{formatDuration(summary.avgDuration)}</p>
          </div>
          <div className="bg-slate-800/50 rounded-lg p-3">
            <p className="text-text-muted text-xs mb-1">数据条目</p>
            <p className="text-xl font-bold text-text">{waveStats.length + durationStats.length}</p>
          </div>
        </div>

        {/* 等级分布 */}
        <div className="border-t border-slate-700 pt-4">
          <h3 className="text-base font-semibold text-text mb-3">等级分布</h3>
          <div className="grid grid-cols-5 gap-3">
            {(['S', 'A', 'B', 'C', 'D'] as const).map(level => (
              <div 
                key={level} 
                className={`rounded-lg p-3 text-center cursor-pointer transition-all ${
                  levelFilter === level ? 'ring-2 ring-primary ring-offset-2 ring-offset-background-light' : ''
                }`}
                onClick={() => setLevelFilter(levelFilter === level ? 'all' : level)}
              >
                <div className={`${GRADE_COLORS[level]} w-10 h-10 rounded-full flex items-center justify-center text-lg font-bold mx-auto mb-1`}>
                  {level}
                </div>
                <p className="text-xl font-bold text-text">{summary.levelDistribution[level]}</p>
                <p className="text-xs text-text-muted mt-1">
                  {level === 'S' && '≥300万'}
                  {level === 'A' && '≥150万'}
                  {level === 'B' && '≥80万'}
                  {level === 'C' && '≥30万'}
                  {level === 'D' && '<30万'}
                </p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 筛选和排序 */}
      <div className="bg-background-light rounded-xl p-4 border border-slate-700">
        <div className="flex flex-wrap items-center gap-4">
          {/* 标签切换 */}
          <div className="flex rounded-lg overflow-hidden border border-slate-700">
            <button
              onClick={() => setActiveTab('wave')}
              className={`px-4 py-2 text-sm font-medium transition-colors ${
                activeTab === 'wave'
                  ? 'bg-accent-gold text-black'
                  : 'bg-background-light text-text-muted hover:bg-slate-700'
              }`}
            >
              音浪数据
            </button>
            <button
              onClick={() => setActiveTab('duration')}
              className={`px-4 py-2 text-sm font-medium transition-colors ${
                activeTab === 'duration'
                  ? 'bg-accent-blue text-black'
                  : 'bg-background-light text-text-muted hover:bg-slate-700'
              }`}
            >
              时长数据
            </button>
          </div>

          {/* 显示日期 */}
          <div className="px-4 py-2 bg-slate-800/50 rounded-lg border border-slate-700">
            <span className="text-text-muted text-sm">日期：</span>
            <span className="text-text font-medium ml-1">{selectedYearMonth || '全部'}</span>
          </div>

          {/* 性别筛选 */}
          <div className="flex items-center gap-2">
            <span className="text-text-muted text-sm">性别：</span>
            <div className="flex rounded-lg overflow-hidden border border-slate-700">
              {[
                { value: 'all' as GenderFilter, label: '全部' },
                { value: 'male' as GenderFilter, label: '男' },
                { value: 'female' as GenderFilter, label: '女' }
              ].map(option => (
                <button
                  key={option.value}
                  onClick={() => setGenderFilter(option.value)}
                  className={`px-3 py-1.5 text-sm transition-colors ${
                    genderFilter === option.value
                      ? 'bg-primary text-white'
                      : 'bg-background-light text-text-muted hover:bg-slate-700'
                  }`}
                >
                  {option.label}
                </button>
              ))}
            </div>
          </div>

          {/* 排序 */}
          <div className="flex items-center gap-2">
            <span className="text-text-muted text-sm">{activeTab === 'wave' ? '音浪排序' : '时长排序'}：</span>
            <div className="flex rounded-lg overflow-hidden border border-slate-700">
              <button
                onClick={() => setSortOrder('desc')}
                className={`px-3 py-1.5 text-sm transition-colors ${
                  sortOrder === 'desc'
                    ? 'bg-accent-gold text-black'
                    : 'bg-background-light text-text-muted hover:bg-slate-700'
                }`}
              >
                从高到低
              </button>
              <button
                onClick={() => setSortOrder('asc')}
                className={`px-3 py-1.5 text-sm transition-colors ${
                  sortOrder === 'asc'
                    ? 'bg-accent-gold text-black'
                    : 'bg-background-light text-text-muted hover:bg-slate-700'
                }`}
              >
                从低到高
              </button>
            </div>
          </div>

          {/* 重置筛选 */}
          {(genderFilter !== 'all' || levelFilter !== 'all') && (
            <button
              onClick={() => {
                setGenderFilter('all')
                setLevelFilter('all')
              }}
              className="px-3 py-1.5 text-sm text-text-muted hover:text-text border border-slate-700 rounded-lg transition-colors"
            >
              重置筛选
            </button>
          )}
        </div>
      </div>

      {/* 数据表格 */}
      <div className="bg-background-light rounded-xl border border-slate-700 overflow-hidden">
        <div className="overflow-auto max-h-[60vh]">
          <table className="w-full">
            <thead className="bg-slate-800/50 sticky top-0 z-10">
              <tr className="text-left text-text-muted text-sm">
                <th className="px-6 py-4 font-medium">排名</th>
                <th className="px-6 py-4 font-medium">等级</th>
                <th className="px-6 py-4 font-medium">主播ID</th>
                <th className="px-6 py-4 font-medium">主播名</th>
                <th className="px-6 py-4 font-medium">性别</th>
                <th className="px-6 py-4 font-medium">{activeTab === 'wave' ? '音浪' : '直播时长'}</th>
                <th className="px-6 py-4 font-medium">数据天数</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-700/50">
              {filteredAndSortedStats.map((stat, index) => {
                return (
                  <tr key={stat.anchor.anchor_id} className="hover:bg-slate-700/30 transition-colors">
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center justify-center w-8 h-8 rounded-full text-sm font-bold ${
                        index < 3 ? 'bg-accent-gold/20 text-accent-gold' : 'bg-slate-700 text-text-muted'
                      }`}>
                        {index + 1}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`${GRADE_COLORS[stat.gradeLetter]} px-3 py-1 rounded-full text-sm font-bold`}>
                        {stat.grade}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-text-muted font-mono text-sm">{stat.anchor.anchor_id}</td>
                    <td className="px-6 py-4 text-text font-medium">{stat.anchor.anchor_name || '-'}</td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 rounded text-xs ${
                        stat.anchor.gender === 'female' 
                          ? 'bg-pink-500/20 text-pink-400' 
                          : 'bg-blue-500/20 text-blue-400'
                      }`}>
                        {stat.anchor.gender === 'female' ? '女' : '男'}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      {activeTab === 'wave' ? (
                        <span className="text-accent-gold font-mono font-bold">
                          {formatWaveValue(stat.totalWave)}
                        </span>
                      ) : (
                        <span className="text-accent-blue font-mono font-bold">
                          {formatDuration(stat.totalDuration)}
                        </span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-text-muted">
                      {activeTab === 'wave' ? `${stat.waveCount} 天` : `${stat.durationCount} 天`}
                    </td>
                  </tr>
                )
              })}
              {filteredAndSortedStats.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-6 py-12 text-center text-text-muted">
                    暂无数据
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="text-sm text-text-muted">
        显示 {filteredAndSortedStats.length} 位主播
      </div>
    </div>
  )
}

export default DataCenter
