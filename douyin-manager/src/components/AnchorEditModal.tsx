import { useState, useEffect } from 'react'
import { Anchor } from '../types'

interface AnchorEditModalProps {
  anchor?: Anchor | null
  onClose: () => void
  onSuccess: () => void
}

function AnchorEditModal({ anchor, onClose, onSuccess }: AnchorEditModalProps) {
  const isEdit = !!anchor
  const [loading, setLoading] = useState(false)
  const [formData, setFormData] = useState({
    anchor_id: '',
    anchor_name: '',
    serial_number: 0,
    gender: 'male' as 'male' | 'female'
  })

  useEffect(() => {
    if (anchor) {
      setFormData({
        anchor_id: anchor.anchor_id,
        anchor_name: anchor.anchor_name || '',
        serial_number: anchor.serial_number || 0,
        gender: anchor.gender || 'male'
      })
    } else {
      // 新建时自动获取下一个序号
      window.electronAPI.getNextSerialNumber().then(result => {
        if (result.success && result.data) {
          setFormData(prev => ({ ...prev, serial_number: result.data! }))
        }
      })
    }
  }, [anchor])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!formData.anchor_id.trim()) {
      alert('请输入主播ID')
      return
    }
    if (!formData.anchor_name.trim()) {
      alert('请输入主播姓名')
      return
    }

    // 如果是编辑模式且ID发生了变化，给出警告
    let shouldProceed = true
    if (isEdit && anchor.anchor_id !== formData.anchor_id) {
      const result = confirm(`您正在更改主播ID从 "${anchor.anchor_id}" 到 "${formData.anchor_id}"。\n\n注意：此操作将同时更新所有相关的统计数据。是否继续？`)
      shouldProceed = result
    }

    if (!shouldProceed) {
      return
    }

    setLoading(true)
    try {
      if (isEdit) {
        await window.electronAPI.updateAnchor(anchor.id, formData)
      } else {
        await window.electronAPI.addAnchor(formData)
      }
      onSuccess()
    } catch (error) {
      alert('操作失败: ' + error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-background-light rounded-xl p-6 w-full max-w-md border border-slate-700">
        <h3 className="text-xl font-semibold text-text mb-6">
          {isEdit ? '编辑主播' : '添加主播'}
        </h3>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm text-text-muted mb-1">主播ID *</label>
            <input
              type="text"
              value={formData.anchor_id}
              onChange={(e) => setFormData({ ...formData, anchor_id: e.target.value })}
              className="w-full px-4 py-2 bg-background border border-slate-700 rounded-lg text-text focus:outline-none focus:border-primary"
              placeholder="请输入主播ID"
            />
          </div>

          <div>
            <label className="block text-sm text-text-muted mb-1">主播姓名 *</label>
            <input
              type="text"
              value={formData.anchor_name}
              onChange={(e) => setFormData({ ...formData, anchor_name: e.target.value })}
              className="w-full px-4 py-2 bg-background border border-slate-700 rounded-lg text-text focus:outline-none focus:border-primary"
              placeholder="请输入主播姓名"
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-text-muted mb-1">序号</label>
              <input
                type="number"
                value={formData.serial_number}
                onChange={(e) => setFormData({ ...formData, serial_number: parseInt(e.target.value) || 0 })}
                className="w-full px-4 py-2 bg-background border border-slate-700 rounded-lg text-text focus:outline-none focus:border-primary"
              />
            </div>

            <div>
              <label className="block text-sm text-text-muted mb-1">性别</label>
              <select
                value={formData.gender}
                onChange={(e) => setFormData({ ...formData, gender: e.target.value as 'male' | 'female' })}
                className="w-full px-4 py-2 bg-background border border-slate-700 rounded-lg text-text focus:outline-none focus:border-primary"
              >
                <option value="male">男</option>
                <option value="female">女</option>
              </select>
            </div>
          </div>

          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 border border-slate-600 rounded-lg text-text-muted hover:bg-slate-800 transition-colors"
            >
              取消
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 px-4 py-2 bg-primary hover:bg-primary-light disabled:opacity-50 rounded-lg text-white font-medium transition-colors"
            >
              {loading ? '保存中...' : '保存'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default AnchorEditModal
