import { useState, useRef, useEffect } from 'react'
import { WaveStat, DurationStat, Anchor } from '../types'
import ExcludedAnchorsModal from './ExcludedAnchorsModal'

interface ExportModalProps {
  type: 'wave' | 'duration'
  waveStats: WaveStat[]
  durationStats: DurationStat[]
  anchors: Anchor[]
  onClose: () => void
}

function ExportModal({ type, waveStats, durationStats, anchors, onClose }: ExportModalProps) {
  const [selectedImage, setSelectedImage] = useState<string | null>(null)
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [showExcludedModal, setShowExcludedModal] = useState(false)
  const [excludedAnchorIds, setExcludedAnchorIds] = useState<string[]>([])
  const fileInputRef = useRef<HTMLInputElement>(null)

  // 从localStorage加载排除的主播ID
  useEffect(() => {
    const savedExcluded = localStorage.getItem('excludedAnchors')
    if (savedExcluded) {
      try {
        const parsed = JSON.parse(savedExcluded)
        if (Array.isArray(parsed)) {
          setExcludedAnchorIds(parsed)
        }
      } catch (e) {
        console.error('Failed to parse excluded anchors:', e)
      }
    }
  }, [])

  const handleSelectImage = async () => {
    try {
      const result = await window.electronAPI.openFileDialog([
        { name: 'Images', extensions: ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'] }
      ])
      if (result && result.length > 0) {
        const imagePath = result[0]
        setSelectedImage(imagePath)
        // 创建预览
        const pathParts = imagePath.split('\\')
        const fileName = pathParts[pathParts.length - 1]
        setImagePreview(fileName)
      }
    } catch (err) {
      console.error('Select image error:', err)
    }
  }

  const handleClearImage = () => {
    setSelectedImage(null)
    setImagePreview(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const handleManageExcluded = () => {
    setShowExcludedModal(true)
  }

  const handleExcludedSave = (excludedIds: string[]) => {
    setExcludedAnchorIds(excludedIds)
    // 保存到localStorage
    localStorage.setItem('excludedAnchors', JSON.stringify(excludedIds))
    setShowExcludedModal(false)
  }

  const handleExportCSV = async () => {
    let data = type === 'wave' ? waveStats : durationStats

    // 过滤排除的主播
    if (excludedAnchorIds.length > 0) {
      const excludedSet = new Set(excludedAnchorIds)
      data = data.filter(stat => !excludedSet.has(stat.anchor_id))
    }

    if (data.length === 0) {
      alert('没有可导出的数据（所有数据都被排除了）')
      return
    }

    const headers = type === 'wave'
      ? ['排名', '主播ID', '主播姓名', '音浪', '日期']
      : ['主播ID', '主播姓名', '时长(分钟)', '日期']

    const rows = data.map(stat => {
      if (type === 'wave') {
        const waveStat = stat as WaveStat
        return [
          waveStat.rank,
          waveStat.anchor_id,
          waveStat.anchor_name || '',
          waveStat.wave_value,
          waveStat.date
        ]
      } else {
        const durationStat = stat as DurationStat
        return [
          durationStat.anchor_id,
          durationStat.anchor_name || '',
          durationStat.duration_minutes,
          durationStat.date
        ]
      }
    })

    const csvContent = [headers, ...rows]
      .map(row => row.map(cell => `"${cell}"`).join(','))
      .join('\n')

    try {
      // 使用保存对话框让用户选择保存位置
      const defaultFileName = `${type === 'wave' ? '音浪' : '时长'}数据_${new Date().toISOString().split('T')[0]}.csv`
      const saveResult = await window.electronAPI.openSaveDialog({
        defaultPath: defaultFileName,
        filters: [{ name: 'CSV Files', extensions: ['csv'] }]
      })

      if (saveResult.canceled || !saveResult.filePath) {
        return
      }

      const filePath = saveResult.filePath

      // 写入 CSV 文件
      const writeResult = await window.electronAPI.writeFile(filePath, '\ufeff' + csvContent)
      if (!writeResult.success) {
        throw new Error(writeResult.error || '写入文件失败')
      }

      // 如果选择了图片，复制图片到同一目录
      if (selectedImage) {
        const dir = await window.electronAPI.getDirName(filePath)
        const imageExt = await window.electronAPI.getExtName(selectedImage)
        const imageName = `${type === 'wave' ? '音浪' : '时长'}配图${imageExt}`
        const imageDest = await window.electronAPI.joinPath(dir, imageName)

        const copyResult = await window.electronAPI.copyFile(selectedImage, imageDest)
        if (!copyResult.success) {
          console.error('Copy image failed:', copyResult.error)
          alert('数据导出成功，但图片复制失败')
          return
        }
      }

      alert('导出成功！')
      onClose()
    } catch (err) {
      console.error('Export error:', err)
      alert('导出失败: ' + String(err))
    }
  }

  // 计算过滤后的数据量
  const filteredData = excludedAnchorIds.length > 0
    ? (type === 'wave' ? waveStats : durationStats).filter(stat => !excludedAnchorIds.includes(stat.anchor_id))
    : (type === 'wave' ? waveStats : durationStats)

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-background-light rounded-xl w-[480px] border border-slate-700">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700">
          <h3 className="text-lg font-semibold text-text">
            导出{type === 'wave' ? '音浪' : '时长'}数据
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

        <div className="p-6 space-y-4">
          <div className="text-center">
            <p className="text-text-muted">
              共 {filteredData.length} 条数据可导出（已排除 {excludedAnchorIds.length} 个主播）
            </p>
          </div>

          {/* 排除主播管理 */}
          <div className="border border-slate-700 rounded-lg p-4">
            <div className="flex items-center justify-between mb-3">
              <p className="text-sm text-text-muted">排除主播管理</p>
              <button
                onClick={handleManageExcluded}
                className="text-sm bg-slate-700 hover:bg-slate-600 px-3 py-1 rounded transition-colors text-text"
              >
                管理
              </button>
            </div>
            {excludedAnchorIds.length > 0 && (
              <div className="text-xs text-text-muted mb-2">
                已排除 {excludedAnchorIds.length} 个主播
              </div>
            )}
          </div>

          {/* 图片选择区域 */}
          <div className="border border-slate-700 rounded-lg p-4">
            <p className="text-sm text-text-muted mb-3">导出配图（可选）</p>
            {!selectedImage ? (
              <button
                onClick={handleSelectImage}
                className="w-full py-4 border-2 border-dashed border-slate-600 hover:border-primary rounded-lg transition-colors flex flex-col items-center gap-2 text-text-muted hover:text-primary"
              >
                <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                <span className="text-sm">点击选择图片</span>
              </button>
            ) : (
              <div className="flex items-center justify-between p-3 bg-slate-800/50 rounded-lg">
                <div className="flex items-center gap-3">
                  <svg className="w-6 h-6 text-accent-green" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span className="text-text text-sm truncate max-w-[200px]">{imagePreview}</span>
                </div>
                <button
                  onClick={handleClearImage}
                  className="text-text-muted hover:text-red-400 transition-colors"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            )}
          </div>

          <button
            onClick={handleExportCSV}
            disabled={filteredData.length === 0}
            className="w-full py-3 bg-accent-green/20 hover:bg-accent-green/30 disabled:opacity-50 disabled:cursor-not-allowed text-accent-green rounded-lg transition-colors flex items-center justify-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            导出 CSV 文件
          </button>

          <div className="p-3 bg-slate-800/50 rounded-lg">
            <p className="text-text-muted text-sm">
              <strong className="text-text">提示：</strong>
              {selectedImage ? '数据将导出为CSV文件，图片将复制到同一目录' : '导出的CSV文件可在Excel中打开查看'}
            </p>
          </div>
        </div>
      </div>

      {showExcludedModal && (
        <ExcludedAnchorsModal
          anchors={anchors}
          excludedAnchorIds={excludedAnchorIds}
          onSave={handleExcludedSave}
          onClose={() => setShowExcludedModal(false)}
        />
      )}
    </div>
  )
}

export default ExportModal
