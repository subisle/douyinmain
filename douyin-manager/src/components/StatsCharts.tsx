import { useRef, useMemo } from 'react'
import ReactECharts from 'echarts-for-react'
import * as echarts from 'echarts'
import { WaveStat, DurationStat, Anchor } from '../types'

interface StatsChartsProps {
  waveStats: WaveStat[]
  durationStats: DurationStat[]
  anchors: Anchor[]
  selectedAnchorIds: string[]
}

// 颜色列表
const COLORS = [
  '#F59E0B', '#3B82F6', '#10B981', '#EF4444', '#8B5CF6',
  '#EC4899', '#06B6D4', '#F97316', '#84CC16', '#6366F1',
  '#14B8A6', '#F43F5E', '#A855F7', '#22C55E', '#0EA5E9'
]

// 格式化日期（处理 Date 对象或字符串）
const formatDateStr = (date: Date | string): string => {
  if (!date) return ''
  if (typeof date === 'string') return date
  return date.toISOString().split('T')[0]
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

function StatsCharts({
  waveStats,
  durationStats,
  anchors,
  selectedAnchorIds
}: StatsChartsProps) {
  const waveChartRef = useRef<ReactECharts>(null)
  const durationChartRef = useRef<ReactECharts>(null)

  // 创建主播ID到名字的映射
  const anchorMap = useMemo(() => {
    const map = new Map<string, string>()
    anchors.forEach(a => map.set(a.anchor_id, a.anchor_name || a.anchor_id))
    // 也从统计数据中获取主播名
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

  // 按主播分组的音浪数据
  const waveDataByAnchor = useMemo(() => {
    const grouped: Record<string, Record<string, number>> = {}
    
    waveStats.forEach(stat => {
      if (!grouped[stat.anchor_id]) {
        grouped[stat.anchor_id] = {}
      }
      const dateStr = formatDateStr(stat.date)
      // 累加而不是覆盖，处理同一主播同一天多条记录
      grouped[stat.anchor_id][dateStr] = (grouped[stat.anchor_id][dateStr] || 0) + stat.wave_value
    })
    
    return grouped
  }, [waveStats])

  // 按主播分组的时长数据
  const durationDataByAnchor = useMemo(() => {
    const grouped: Record<string, Record<string, number>> = {}
    
    durationStats.forEach(stat => {
      if (!grouped[stat.anchor_id]) {
        grouped[stat.anchor_id] = {}
      }
      const dateStr = formatDateStr(stat.date)
      // 累加而不是覆盖，处理同一主播同一天多条记录
      grouped[stat.anchor_id][dateStr] = (grouped[stat.anchor_id][dateStr] || 0) + stat.duration_minutes
    })
    
    return grouped
  }, [durationStats])

  // 获取所有日期
  const allDates = useMemo(() => {
    const dates = new Set<string>()
    waveStats.forEach(s => dates.add(formatDateStr(s.date)))
    durationStats.forEach(s => dates.add(formatDateStr(s.date)))
    return Array.from(dates).sort((a, b) => new Date(a).getTime() - new Date(b).getTime())
  }, [waveStats, durationStats])

  // 聚合音浪数据（总和）
  const aggregatedWaveData = useMemo(() => {
    const grouped: Record<string, number> = {}

    waveStats.forEach(stat => {
      const dateStr = formatDateStr(stat.date)
      if (!grouped[dateStr]) {
        grouped[dateStr] = 0
      }
      grouped[dateStr] += stat.wave_value
    })

    return Object.entries(grouped)
      .sort(([a], [b]) => new Date(a).getTime() - new Date(b).getTime())
      .map(([date, value]) => ({ date, value }))
  }, [waveStats])

  // 聚合时长数据（总和）
  const aggregatedDurationData = useMemo(() => {
    const grouped: Record<string, number> = {}

    durationStats.forEach(stat => {
      const dateStr = formatDateStr(stat.date)
      if (!grouped[dateStr]) {
        grouped[dateStr] = 0
      }
      grouped[dateStr] += stat.duration_minutes
    })

    return Object.entries(grouped)
      .sort(([a], [b]) => new Date(a).getTime() - new Date(b).getTime())
      .map(([date, value]) => ({ date, value: Math.round(value / 60) }))
  }, [durationStats])

  // 生成音浪图表系列
  const waveSeries = useMemo(() => {
    if (selectedAnchorIds.length === 0) {
      // 显示总和
      return [{
        name: '总和',
        type: 'line' as const,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: aggregatedWaveData.map(d => d.value),
        lineStyle: { color: COLORS[0], width: 3 },
        itemStyle: { color: COLORS[0] },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(245, 158, 11, 0.3)' },
            { offset: 1, color: 'rgba(245, 158, 11, 0.05)' }
          ])
        }
      }]
    }
    
    // 显示选中的主播
    return selectedAnchorIds.map((anchorId, index) => {
      const anchorData = waveDataByAnchor[anchorId] || {}
      const color = COLORS[index % COLORS.length]
      const anchorName = anchorMap.get(anchorId) || anchorId
      
      return {
        name: anchorName,
        type: 'line' as const,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: allDates.map(date => anchorData[date] || 0),
        lineStyle: { color, width: 2 },
        itemStyle: { color }
      }
    })
  }, [selectedAnchorIds, aggregatedWaveData, waveDataByAnchor, allDates, anchorMap])

  // 生成时长图表系列
  const durationSeries = useMemo(() => {
    if (selectedAnchorIds.length === 0) {
      // 显示总和
      return [{
        name: '总和',
        type: 'line' as const,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: aggregatedDurationData.map(d => d.value),
        lineStyle: { color: COLORS[1], width: 3 },
        itemStyle: { color: COLORS[1] },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
          ])
        }
      }]
    }
    
    // 显示选中的主播
    return selectedAnchorIds.map((anchorId, index) => {
      const anchorData = durationDataByAnchor[anchorId] || {}
      const color = COLORS[index % COLORS.length]
      const anchorName = anchorMap.get(anchorId) || anchorId
      
      return {
        name: anchorName,
        type: 'line' as const,
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: allDates.map(date => Math.round((anchorData[date] || 0) / 60)),
        lineStyle: { color, width: 2 },
        itemStyle: { color }
      }
    })
  }, [selectedAnchorIds, aggregatedDurationData, durationDataByAnchor, allDates, anchorMap])

  const waveChartOption = useMemo(() => {
    const xData = selectedAnchorIds.length === 0 
      ? aggregatedWaveData.map(d => d.date) 
      : (allDates.length > 0 ? allDates : ['暂无数据'])
    
    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#1E293B',
        borderColor: '#475569',
        borderWidth: 1,
        padding: [10, 15],
        textStyle: { color: '#F1F5F9', fontSize: 13 },
        formatter: (params: any[]) => {
          if (!params || params.length === 0) return ''
          const date = params[0].axisValue
          let html = `<div style="font-weight:600;margin-bottom:8px;">${date}</div>`
          params.forEach(p => {
            html += `<div style="display:flex;align-items:center;gap:8px;margin:4px 0;">
              ${p.marker} <span style="color:#94A3B8">${p.seriesName}:</span> 
              <span style="font-weight:600;color:#F59E0B">${formatWaveValue(p.value)}</span>
            </div>`
          })
          return html
        }
      },
      legend: {
        type: 'scroll',
        top: 5,
        textStyle: { color: '#94A3B8', fontSize: 12 },
        pageTextStyle: { color: '#94A3B8' },
        pageIconColor: '#F59E0B',
        pageIconInactiveColor: '#475569'
      },
      grid: {
        left: '2%',
        right: '3%',
        bottom: '8%',
        top: 60,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: xData,
        axisLine: { lineStyle: { color: '#475569' } },
        axisLabel: { 
          color: '#94A3B8',
          fontSize: 11,
          rotate: xData.length > 15 ? 45 : 0
        },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisLabel: { 
          color: '#94A3B8', 
          fontSize: 11,
          formatter: (v: number) => formatWaveValue(v) 
        },
        splitLine: { lineStyle: { color: '#334155', type: 'dashed' } }
      },
      series: waveSeries.length > 0 ? waveSeries : [{
        name: '暂无数据',
        type: 'line',
        data: xData.map(() => 0)
      }]
    }
  }, [waveSeries, aggregatedWaveData, allDates, selectedAnchorIds])

  const durationChartOption = useMemo(() => {
    const xData = selectedAnchorIds.length === 0
      ? aggregatedDurationData.map(d => d.date)
      : (allDates.length > 0 ? allDates : ['暂无数据'])

    return {
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#1E293B',
        borderColor: '#475569',
        borderWidth: 1,
        padding: [10, 15],
        textStyle: { color: '#F1F5F9', fontSize: 13 },
        formatter: (params: any[]) => {
          if (!params || params.length === 0) return ''
          const date = params[0].axisValue
          let html = `<div style="font-weight:600;margin-bottom:8px;">${date}</div>`
          params.forEach(p => {
            html += `<div style="display:flex;align-items:center;gap:8px;margin:4px 0;">
              ${p.marker} <span style="color:#94A3B8">${p.seriesName}:</span> 
              <span style="font-weight:600;color:#3B82F6">${p.value} 小时</span>
            </div>`
          })
          return html
        }
      },
      legend: {
        type: 'scroll',
        top: 5,
        textStyle: { color: '#94A3B8', fontSize: 12 },
        pageTextStyle: { color: '#94A3B8' },
        pageIconColor: '#3B82F6',
        pageIconInactiveColor: '#475569'
      },
      grid: {
        left: '2%',
        right: '3%',
        bottom: '8%',
        top: 60,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: xData,
        axisLine: { lineStyle: { color: '#475569' } },
        axisLabel: { 
          color: '#94A3B8',
          fontSize: 11,
          rotate: xData.length > 15 ? 45 : 0
        },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisLabel: { 
          color: '#94A3B8', 
          fontSize: 11,
          formatter: '{value}h' 
        },
        splitLine: { lineStyle: { color: '#334155', type: 'dashed' } }
      },
      series: durationSeries.length > 0 ? durationSeries : [{
        name: '暂无数据',
        type: 'line',
        data: xData.map(() => 0)
      }]
    }
  }, [durationSeries, aggregatedDurationData, allDates, selectedAnchorIds])

  const handleExportWave = () => {
    if (waveChartRef.current) {
      const url = waveChartRef.current.getEchartsInstance().getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#0F172A'
      })
      const link = document.createElement('a')
      link.download = `音浪趋势图_${new Date().toISOString().split('T')[0]}.png`
      link.href = url
      link.click()
    }
  }

  const handleExportDuration = () => {
    if (durationChartRef.current) {
      const url = durationChartRef.current.getEchartsInstance().getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#0F172A'
      })
      const link = document.createElement('a')
      link.download = `时长趋势图_${new Date().toISOString().split('T')[0]}.png`
      link.href = url
      link.click()
    }
  }

  return (
    <div className="space-y-6">

      {/* Charts - 上下布局 */}
      {/* Wave Chart */}
      <div className="bg-background-light rounded-xl p-6 border border-slate-700">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-text flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-amber-500"></span>
            音浪趋势
          </h3>
          <button
            onClick={handleExportWave}
            className="px-3 py-1.5 bg-accent-gold/20 hover:bg-accent-gold/30 text-accent-gold rounded-lg text-sm flex items-center gap-1 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            导出图片
          </button>
        </div>
        <div className="h-96">
          <ReactECharts ref={waveChartRef} option={waveChartOption} style={{ height: '100%' }} />
        </div>
      </div>

      {/* Duration Chart */}
      <div className="bg-background-light rounded-xl p-6 border border-slate-700">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-text flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-blue-500"></span>
            时长趋势
          </h3>
          <button
            onClick={handleExportDuration}
            className="px-3 py-1.5 bg-accent-blue/20 hover:bg-accent-blue/30 text-accent-blue rounded-lg text-sm flex items-center gap-1 transition-colors"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            导出图片
          </button>
        </div>
        <div className="h-96">
          <ReactECharts ref={durationChartRef} option={durationChartOption} style={{ height: '100%' }} />
        </div>
      </div>
    </div>
  )
}

export default StatsCharts
