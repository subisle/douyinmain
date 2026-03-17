import { useState, useMemo } from 'react'
import { Anchor } from '../types'

interface ExcludedAnchorsModalProps {
  anchors: Anchor[]
  excludedAnchorIds: string[]
  onSave: (excludedIds: string[]) => void
  onClose: () => void
}

function ExcludedAnchorsModal({ anchors, excludedAnchorIds, onSave, onClose }: ExcludedAnchorsModalProps) {
  const [searchTerm, setSearchTerm] = useState('')
  const [selectedAnchors, setSelectedAnchors] = useState<string[]>([...excludedAnchorIds])

  // 过滤主播列表
  const filteredAnchors = useMemo(() => {
    return anchors.filter(anchor =>
      anchor.anchor_id.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (anchor.anchor_name && anchor.anchor_name.toLowerCase().includes(searchTerm.toLowerCase()))
    )
  }, [anchors, searchTerm])

  const handleToggleAnchor = (anchorId: string) => {
    setSelectedAnchors(prev =>
      prev.includes(anchorId)
        ? prev.filter(id => id !== anchorId)
        : [...prev, anchorId]
    )
  }

  const handleSelectAll = () => {
    const allAnchorIds = anchors.map(anchor => anchor.anchor_id)
    setSelectedAnchors(allAnchorIds)
  }

  const handleClearAll = () => {
    setSelectedAnchors([])
  }

  const handleSave = () => {
    onSave(selectedAnchors)
    onClose()
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-background-light rounded-xl w-[600px] max-h-[80vh] border border-slate-700 flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700">
          <h3 className="text-lg font-semibold text-text">
            选择要排除的主播
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

        <div className="p-6 flex-1 overflow-hidden flex flex-col">
          {/* 搜索和操作按钮 */}
          <div className="flex items-center gap-3 mb-4">
            <div className="relative flex-1">
              <input
                type="text"
                placeholder="搜索主播ID或姓名..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full px-4 py-2 pl-10 bg-background border border-slate-700 rounded-lg text-text placeholder-text-muted focus:outline-none focus:border-primary"
              />
              <svg className="w-5 h-5 text-text-muted absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <button
              onClick={handleSelectAll}
              className="px-3 py-2 bg-slate-700 hover:bg-slate-600 rounded-lg text-text text-sm transition-colors"
            >
              全选
            </button>
            <button
              onClick={handleClearAll}
              className="px-3 py-2 bg-slate-700 hover:bg-slate-600 rounded-lg text-text text-sm transition-colors"
            >
              清空
            </button>
          </div>

          {/* 统计信息 */}
          <div className="text-sm text-text-muted mb-3">
            已选择 <span className="text-primary font-medium">{selectedAnchors.length}</span> 个主播
          </div>

          {/* 主播列表 */}
          <div className="border border-slate-700 rounded-lg overflow-hidden flex-1">
            <div className="max-h-96 overflow-y-auto">
              <table className="w-full">
                <thead className="bg-slate-800/50 sticky top-0 z-10">
                  <tr className="text-left text-text-muted text-sm">
                    <th className="px-4 py-3 font-medium w-12"></th>
                    <th className="px-4 py-3 font-medium">主播ID</th>
                    <th className="px-4 py-3 font-medium">主播姓名</th>
                    <th className="px-4 py-3 font-medium">序号</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAnchors.map((anchor) => {
                    const isSelected = selectedAnchors.includes(anchor.anchor_id)
                    return (
                      <tr
                        key={anchor.id}
                        className="border-t border-slate-700/50 hover:bg-slate-800/30"
                      >
                        <td className="px-4 py-3">
                          <input
                            type="checkbox"
                            checked={isSelected}
                            onChange={() => handleToggleAnchor(anchor.anchor_id)}
                            className="w-4 h-4 text-primary bg-background border-slate-600 rounded focus:ring-primary focus:ring-offset-background"
                          />
                        </td>
                        <td className="px-4 py-3 text-text font-mono text-sm">{anchor.anchor_id}</td>
                        <td className="px-4 py-3 text-text">{anchor.anchor_name || '-'}</td>
                        <td className="px-4 py-3 text-text-muted">{anchor.serial_number || 0}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        {/* 底部按钮 */}
        <div className="flex gap-3 px-6 py-4 border-t border-slate-700">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2 border border-slate-600 rounded-lg text-text-muted hover:bg-slate-800 transition-colors"
          >
            取消
          </button>
          <button
            onClick={handleSave}
            className="flex-1 px-4 py-2 bg-primary hover:bg-primary-light rounded-lg text-white font-medium transition-colors"
          >
            保存
          </button>
        </div>
      </div>
    </div>
  )
}

export default ExcludedAnchorsModal
