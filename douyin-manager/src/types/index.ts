export interface Anchor {
  id: number
  anchor_id: string
  anchor_name: string
  serial_number?: number
  gender?: 'male' | 'female'
  created_at?: string
  updated_at?: string
}

export interface WaveStat {
  id: number
  anchor_id: string
  anchor_name?: string
  date: string | Date
  wave_value: number
  rank: number
}

export interface DurationStat {
  id: number
  anchor_id: string
  anchor_name?: string
  date: string | Date
  duration_minutes: number
}

export interface StatsFilters {
  anchor_id?: string
  startDate?: string
  endDate?: string
}

export type TimeRange = 'day' | 'week' | 'month' | 'custom'

export interface ChartData {
  date: string
  value: number
  anchorName?: string
}
