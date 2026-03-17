import { useState, useEffect, useRef } from 'react'
import Papa from 'papaparse'
import { Anchor } from '../types'
import Toast from './Toast'

interface AnchorImportModalProps {
  anchors: Anchor[]
  onClose: () => void
  onSuccess: () => void
}

interface PreviewRow {
  anchor_id: string
  anchor_name: string
  gender?: 'male' | 'female'
  serial_number?: number
  matched: boolean // 是否已存在于主播列表中
  selected: boolean // 用户是否选择了这一行
  modified_name?: string // 用户手动修改后的名字
}

function AnchorImportModal({ anchors, onClose, onSuccess }: AnchorImportModalProps) {
  const [step, setStep] = useState<'select' | 'preview'>('select')
  const [previewData, setPreviewData] = useState<PreviewRow[]>([])
  const [error, setError] = useState('')
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null)
  const [fileContent, setFileContent] = useState<string>('')
  const processedFile = useRef<string>('')

  // 创建主播ID映射，用于快速查找
  const anchorMap = new Map(anchors.map(a => [a.anchor_id, a]))

  // 处理文件内容
  const processFileContent = (content: string) => {
    setError('')
    setFileContent(content)

    Papa.parse(content, {
      header: true,
      skipEmptyLines: true,
      complete: (results) => {
        try {
          const data = results.data as any[]
          
          if (data.length === 0) {
            setError('CSV 文件为空或格式不正确')
            return
          }
          
          // 检测必要字段
          const headers = Object.keys(data[0] || {})
          const hasIdField = headers.includes('主播ID') || headers.includes('anchor_id')
          const hasNameField = headers.includes('主播名') || headers.includes('主播昵称') || headers.includes('anchor_name')
          
          if (!hasIdField) {
            setError('缺少"主播ID"列')
            return
          }
          if (!hasNameField) {
            setError('缺少"主播名"或"主播昵称"列')
            return
          }
          
          // 解析主播数据
          const preview: PreviewRow[] = data
            .filter(row => row['主播ID'] || row['anchor_id'])
            .map(row => {
              const anchorId = String((row['主播ID'] || row['anchor_id']) || '').trim()
              const originalName = String((row['主播名'] || row['主播昵称'] || row['anchor_name']) || '').trim()
              const gender = row['性别'] || row['gender'] || row['gender_text'] || row['gender_type'] || 'male'
              const serialNumber = row['序号'] || row['serial_number'] || row['number']

              // 检查是否已存在于主播列表中
              const exists = anchorMap.has(anchorId)
              
              return {
                anchor_id: anchorId,
                anchor_name: originalName,
                gender: gender === '女' || gender === 'female' ? 'female' : 'male' as 'male' | 'female',
                serial_number: serialNumber ? Number(serialNumber) : undefined,
                matched: exists, // matched 表示是否已存在
                selected: !exists, // 默认选择不存在的主播
                modified_name: originalName // 初始时使用原名
              }
            })
            .filter(row => row.anchor_id) // 过滤掉没有ID的数据
          
          setPreviewData(preview)
          setStep('preview')
        } catch (err) {
          setError('解析CSV文件失败: ' + String(err))
        }
      },
      error: (error: any) => {
        setError('解析CSV文件失败: ' + error.message)
      }
    })
  }

  const handleSelectFile = async () => {
    try {
      setError('')
      setPreviewData([])

      const filePaths = await window.electronAPI.openFileDialog([
        { name: 'CSV Files', extensions: ['csv'] }
      ])

      if (!filePaths || filePaths.length === 0) return

      const path = filePaths[0]
      const result = await window.electronAPI.readFile(path)
      
      if (!result.success || !result.data) {
        setError('读取文件失败: ' + result.error)
        return
      }

      processFileContent(result.data)
    } catch (err) {
      setError('选择文件失败')
    }
  }

  // 处理拖拽导入的文件
  useEffect(() => {
    const handleDrop = (event: DragEvent) => {
      event.preventDefault()
      if (event.dataTransfer?.files.length) {
        const file = event.dataTransfer.files[0]
        if (file.name.endsWith('.csv')) {
          const reader = new FileReader()
          reader.onload = (e) => {
            const content = e.target?.result as string
            if (content && content !== processedFile.current) {
              processedFile.current = content
              processFileContent(content)
            }
          }
          reader.readAsText(file)
        }
      }
    }

    const handleDragOver = (event: DragEvent) => {
      event.preventDefault()
    }

    document.addEventListener('dragover', handleDragOver)
    document.addEventListener('drop', handleDrop)
    return () => {
      document.removeEventListener('dragover', handleDragOver)
      document.removeEventListener('drop', handleDrop)
    }
  }, [])

  const handleConfirmImport = async () => {
    // 只导入用户选择且不存在的主播
    const selectedData = previewData.filter(row => row.selected && !row.matched)
    
    if (selectedData.length === 0) {
      setToast({ message: '没有可导入的主播（请选择不存在的主播）', type: 'error' })
      return
    }

    try {
      // 计算起始序列号
      const nextSerialResult = await window.electronAPI.getNextSerialNumber()
      const nextSerial = nextSerialResult.data || 1
      
      for (let i = 0; i < selectedData.length; i++) {
        const row = selectedData[i]
        const serialNumber = nextSerial + i
        const anchorData = {
          anchor_id: row.anchor_id,
          anchor_name: row.modified_name || row.anchor_name,
          serial_number: serialNumber,
          gender: row.gender
        }

        const result = await window.electronAPI.addAnchor(anchorData)
        if (!result.success) {
          throw new Error(`添加主播 ${row.anchor_name} 失败: ${result.error}`)
        }
      }

      setToast({ message: `成功导入 ${selectedData.length} 位主播！正在同步到数据库...`, type: 'success' })
      setTimeout(() => {
        onSuccess()
      }, 1500)
    } catch (err: any) {
      setError('导入失败: ' + err.message)
    }
  }

  const handleRowSelectionChange = (index: number, selected: boolean) => {
    const newData = [...previewData]
    newData[index].selected = selected
    setPreviewData(newData)
  }

  const handleNameChange = (index: number, newName: string) => {
    const newData = [...previewData]
    newData[index].modified_name = newName
    setPreviewData(newData)
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-background-light rounded-xl w-[1000px] max-h-[90vh] border border-slate-700 flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700 flex-shrink-0">
          <h3 className="text-lg font-semibold text-text">从CSV文件导入主播</h3>
          <button
            onClick={onClose}
            className="text-text-muted hover:text-text transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="p-6 flex-1 overflow-hidden">
          {step === 'select' && (
            <div className="space-y-6 h-full flex flex-col">
              <div
                onClick={handleSelectFile}
                className="border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-colors border-slate-600 hover:border-primary hover:bg-slate-800/30 flex-1 flex flex-col items-center justify-center"
              >
                <svg className="w-12 h-12 mx-auto text-text-muted mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                </svg>
                <p className="text-text font-medium">点击选择CSV文件</p>
                <p className="text-text-muted text-sm mt-1">支持格式：主播榜、主播概览列表等CSV文件</p>
                <p className="text-text-muted text-xs mt-2">文件需包含"主播ID"和"主播名"列</p>
              </div>
              
              <div className="text-center text-text-muted text-sm">
                或者直接将CSV文件拖拽到窗口中
              </div>
            </div>
          )}

          {step === 'preview' && (
            <div className="space-y-4 h-full flex flex-col">
              {/* 统计信息 */}
              <div className="flex items-center justify-between flex-shrink-0">
                <div className="space-y-1">
                  <div className="text-text text-sm font-medium">共 {previewData.length} 条数据</div>
                  <div className="text-text-muted text-xs flex gap-4">
                    <span>已存在 {previewData.filter(r => r.matched).length} 位</span>
                    <span>可新增 {previewData.filter(r => !r.matched && r.selected).length} 位</span>
                  </div>
                </div>
                <div className="text-right text-sm text-text-muted">
                  已选择 {previewData.filter(r => r.selected && !r.matched).length} 位主播
                </div>
              </div>

              {/* 数据预览表格 */}
              <div className="border border-slate-700 rounded-lg overflow-hidden flex-1 flex flex-col">
                <div className="overflow-auto flex-1">
                  <table className="w-full">
                    <thead className="bg-slate-800/50 sticky top-0 z-10">
                      <tr className="text-left text-text-muted text-sm">
                        <th className="px-4 py-3 font-medium whitespace-nowrap w-12">选择</th>
                        <th className="px-4 py-3 font-medium whitespace-nowrap">主播ID</th>
                        <th className="px-4 py-3 font-medium whitespace-nowrap">主播姓名</th>
                        <th className="px-4 py-3 font-medium whitespace-nowrap">性别</th>
                        <th className="px-4 py-3 font-medium whitespace-nowrap">状态</th>
                      </tr>
                    </thead>
                    <tbody>
                      {previewData.map((row, index) => (
                        <tr 
                          key={index} 
                          className={`border-t border-slate-700/50 ${row.matched ? 'bg-red-500/10' : 'hover:bg-slate-800/30'} transition-colors`}
                        >
                          <td className="px-4 py-2 whitespace-nowrap">
                            {!row.matched && (
                              <input
                                type="checkbox"
                                checked={row.selected}
                                onChange={(e) => handleRowSelectionChange(index, e.target.checked)}
                                className="w-4 h-4 text-primary rounded focus:ring-primary"
                              />
                            )}
                          </td>
                          <td className="px-4 py-2 text-text-muted font-mono text-sm whitespace-nowrap">{row.anchor_id}</td>
                          <td className="px-4 py-2 whitespace-nowrap">
                            {row.matched ? (
                              <span className="text-red-400 line-through">{row.anchor_name}</span>
                            ) : (
                              <input
                                type="text"
                                value={row.modified_name || row.anchor_name}
                                onChange={(e) => handleNameChange(index, e.target.value)}
                                className="w-full px-2 py-1 bg-background border border-slate-600 rounded text-text focus:outline-none focus:border-primary text-sm"
                                placeholder="请输入主播姓名"
                              />
                            )}
                          </td>
                          <td className="px-4 py-2 whitespace-nowrap">
                            <span className={`px-2 py-1 rounded text-xs ${
                              row.gender === 'female' ? 'bg-pink-500/20 text-pink-400' : 'bg-blue-500/20 text-blue-400'
                            }`}>
                              {row.gender === 'female' ? '女' : '男'}
                            </span>
                          </td>
                          <td className="px-4 py-2 whitespace-nowrap">
                            {row.matched ? (
                              <span className="text-red-400 text-xs">已存在</span>
                            ) : (
                              <span className="text-green-400 text-xs">可添加</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* 说明文字 */}
              <div className="text-xs text-text-muted space-y-1">
                <p>• 红色背景的主播已存在于主播列表中，无法选择</p>
                <p>• 可以手动修改主播姓名</p>
                <p>• 只能选择不存在的主播进行导入</p>
              </div>
            </div>
          )}

          {error && (
            <div className="mt-4 p-3 bg-red-500/20 border border-red-500/50 rounded-lg text-red-400 text-sm whitespace-pre-line">
              {error}
            </div>
          )}
        </div>

        {step === 'preview' && (
          <div className="flex gap-3 px-6 py-4 border-t border-slate-700 flex-shrink-0">
            <button
              onClick={() => setStep('select')}
              className="flex-1 px-4 py-2 border border-slate-600 rounded-lg text-text-muted hover:bg-slate-800 transition-colors"
            >
              重新选择
            </button>
            <button
              onClick={handleConfirmImport}
              disabled={previewData.filter(r => r.selected && !r.matched).length === 0}
              className="flex-1 px-4 py-2 bg-primary hover:bg-primary-light disabled:opacity-50 rounded-lg text-white font-medium transition-colors"
            >
              确认导入 ({previewData.filter(r => r.selected && !r.matched).length} 位)
            </button>
          </div>
        )}
      </div>
      
      {/* Toast 提示 */}
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  )
}

export default AnchorImportModal
