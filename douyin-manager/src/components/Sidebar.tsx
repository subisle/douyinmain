import { useState } from 'react'

type Page = 'dashboard' | 'datacenter' | 'anchors' | 'stats'

interface SidebarProps {
  currentPage: Page
  onPageChange: (page: Page) => void
  onCollapsedChange?: (collapsed: boolean) => void
  onDataCleared?: () => void
}

const menuItems = [
  {
    id: 'dashboard' as Page,
    label: '数据概览',
    shortLabel: '概览',
    image: '📊',
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
      </svg>
    )
  },
  {
    id: 'datacenter' as Page,
    label: '数据中心',
    shortLabel: '数据',
    image: '🗄️',
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
      </svg>
    )
  },
  {
    id: 'anchors' as Page,
    label: '主播列表',
    shortLabel: '主播',
    image: '👥',
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>
    )
  },
  {
    id: 'stats' as Page,
    label: '数据统计',
    shortLabel: '统计',
    image: '📈',
    icon: (
      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
      </svg>
    )
  }
]

function Sidebar({ currentPage, onPageChange, onCollapsedChange, onDataCleared }: SidebarProps) {
  const [isCollapsed, setIsCollapsed] = useState(false)
  const [showClearModal, setShowClearModal] = useState(false)
  const [clearing, setClearing] = useState(false)
  const [clearType, setClearType] = useState<'local' | 'remote' | 'both'>('local')

  const toggleCollapsed = () => {
    const newCollapsed = !isCollapsed
    setIsCollapsed(newCollapsed)
    onCollapsedChange?.(newCollapsed)
  }

  const handleClearData = async () => {
    setClearing(true)
    try {
      const result = await window.electronAPI.clearStatsData(clearType)
      if (result.success) {
        setShowClearModal(false)
        // 显示警告（如果有）
        if (result.warnings && result.warnings.length > 0) {
          alert('警告:\n' + result.warnings.join('\n'))
        }
        onDataCleared?.()
      } else {
        alert('清除失败: ' + (result.error || '未知错误'))
      }
    } catch (err) {
      alert('清除失败: ' + String(err))
    } finally {
      setClearing(false)
    }
  }

  return (
    <>
      <aside className={`fixed left-0 top-16 bottom-0 bg-background-light border-r border-slate-700 p-4 flex flex-col transition-all duration-300 ${isCollapsed ? 'w-16' : 'w-64'}`}>
        <nav className="space-y-2 flex-1">
          {menuItems.map((item) => (
            <button
              key={item.id}
              onClick={() => onPageChange(item.id)}
              className={`w-full flex flex-col items-center justify-center gap-2 px-4 py-3 rounded-lg transition-all duration-200 ${
                currentPage === item.id
                  ? 'bg-primary text-white'
                  : 'text-text-muted hover:bg-slate-700 hover:text-text'
              }`}
              title={isCollapsed ? item.label : undefined}
            >
              <span className="text-2xl">{item.image}</span>
              {!isCollapsed && (
                <span className="font-medium text-sm">{item.label}</span>
              )}
            </button>
          ))}
        </nav>

        {/* 底部按钮区域 */}
        <div className="border-t border-slate-700 pt-4 space-y-2">
          {/* 清除数据按钮 */}
          <button
            onClick={() => setShowClearModal(true)}
            className={`w-full flex items-center justify-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 ${
              isCollapsed 
                ? 'bg-red-500/20 text-red-400 hover:bg-red-500/30' 
                : 'text-text-muted hover:bg-red-500/10 hover:text-red-400'
            }`}
            title={isCollapsed ? '清除数据' : undefined}
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            {!isCollapsed && <span className="font-medium">清除数据</span>}
          </button>

          {/* 收缩按钮 - 收缩后更明显 */}
          <button
            onClick={toggleCollapsed}
            className={`w-full flex items-center justify-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 ${
              isCollapsed 
                ? 'bg-primary hover:bg-primary-light text-white' 
                : 'text-text-muted hover:bg-slate-700 hover:text-text'
            }`}
            title={isCollapsed ? '展开侧边栏' : '收缩侧边栏'}
          >
            {isCollapsed ? (
              <svg className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" d="M13 5l7 7-7 7M5 5l7 7-7 7" />
              </svg>
            ) : (
              <>
                <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth={2} viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
                </svg>
                <span className="font-medium">收缩侧边栏</span>
              </>
            )}
          </button>
        </div>
      </aside>

      {/* 清除数据弹窗 */}
      {showClearModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-background-light rounded-xl w-[400px] border border-slate-700">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700">
              <h3 className="text-lg font-semibold text-text">清除数据</h3>
              <button
                onClick={() => setShowClearModal(false)}
                className="text-text-muted hover:text-text transition-colors"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div className="p-6 space-y-4">
              <p className="text-text-muted text-sm">选择要清除的数据范围：</p>

              <div className="space-y-3">
                <label className="flex items-center gap-3 cursor-pointer p-3 rounded-lg border border-slate-700 hover:bg-slate-700/30 transition-colors">
                  <input
                    type="radio"
                    name="clearType"
                    value="local"
                    checked={clearType === 'local'}
                    onChange={() => setClearType('local')}
                    className="w-4 h-4 text-primary"
                  />
                  <div>
                    <span className="text-text font-medium">只清除本地数据</span>
                    <p className="text-text-muted text-xs mt-1">仅清除本地数据库中的音浪和时长统计数据</p>
                  </div>
                </label>

                <label className="flex items-center gap-3 cursor-pointer p-3 rounded-lg border border-slate-700 hover:bg-slate-700/30 transition-colors">
                  <input
                    type="radio"
                    name="clearType"
                    value="remote"
                    checked={clearType === 'remote'}
                    onChange={() => setClearType('remote')}
                    className="w-4 h-4 text-primary"
                  />
                  <div>
                    <span className="text-text font-medium">只清除远程数据</span>
                    <p className="text-text-muted text-xs mt-1">仅清除远程数据库中的音浪和时长统计数据</p>
                  </div>
                </label>

                <label className="flex items-center gap-3 cursor-pointer p-3 rounded-lg border border-red-500/30 hover:bg-red-500/10 transition-colors">
                  <input
                    type="radio"
                    name="clearType"
                    value="both"
                    checked={clearType === 'both'}
                    onChange={() => setClearType('both')}
                    className="w-4 h-4 text-red-500"
                  />
                  <div>
                    <span className="text-red-400 font-medium">清除全部数据</span>
                    <p className="text-text-muted text-xs mt-1">同时清除本地和远程数据库中的所有统计数据</p>
                  </div>
                </label>
              </div>

              <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-lg p-3">
                <p className="text-yellow-400 text-sm flex items-center gap-2">
                  <svg className="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                  此操作不可撤销，请谨慎操作
                </p>
              </div>
            </div>

            <div className="flex gap-3 px-6 py-4 border-t border-slate-700">
              <button
                onClick={() => setShowClearModal(false)}
                className="flex-1 px-4 py-2 border border-slate-600 rounded-lg text-text-muted hover:bg-slate-700 transition-colors"
              >
                取消
              </button>
              <button
                onClick={handleClearData}
                disabled={clearing}
                className="flex-1 px-4 py-2 bg-red-500 hover:bg-red-600 disabled:opacity-50 rounded-lg text-white font-medium transition-colors"
              >
                {clearing ? '清除中...' : '确认清除'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

export default Sidebar
