import { useState, useRef } from 'react'
import html2canvas from 'html2canvas'
import { WaveStat } from '../types'

interface PosterExportProps {
  data: WaveStat[]
  date: string
  onClose: () => void
}

// 等级计算函数
const getGrade = (waveValue: number): string => {
  if (waveValue >= 500000) return 'S+'
  if (waveValue >= 300000) return 'S'
  if (waveValue >= 200000) return 'A+'
  if (waveValue >= 100000) return 'A'
  if (waveValue >= 50000) return 'B+'
  if (waveValue >= 30000) return 'B'
  if (waveValue >= 10000) return 'C+'
  if (waveValue >= 5000) return 'C'
  return 'D'
}

// 格式化音浪数值
const formatWave = (num: number): string => {
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

function PosterExport({ data, date, onClose }: PosterExportProps) {
  const [companyName] = useState('星嗨艺创')
  const [eventTitle] = useState('每日音浪排行')
  const [exporting, setExporting] = useState(false)
  const posterRef = useRef<HTMLDivElement>(null)

  // 按音浪值排序
  const sortedData = [...data].sort((a, b) => b.wave_value - a.wave_value)
  
  // 前三名
  const topThree = sortedData.slice(0, 3)
  // 其余排名
  const restList = sortedData.slice(3)

  const handleExport = async () => {
    if (!posterRef.current) return
    
    setExporting(true)
    try {
      const canvas = await html2canvas(posterRef.current, {
        backgroundColor: '#000000',
        scale: 2,
        useCORS: true,
        logging: false
      })
      
      // 转换为图片并下载
      const link = document.createElement('a')
      link.download = `音浪排行榜_${date}.png`
      link.href = canvas.toDataURL('image/png')
      link.click()
      
      onClose()
    } catch (err) {
      console.error('Export error:', err)
      alert('导出失败: ' + String(err))
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 overflow-auto py-4">
      <div className="bg-background-light rounded-xl w-[850px] max-h-[95vh] border border-slate-700 flex flex-col">
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700 shrink-0">
          <h3 className="text-lg font-semibold text-text">导出排行榜海报</h3>
          <button
            onClick={onClose}
            className="text-text-muted hover:text-text transition-colors"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="p-4 overflow-auto flex-1">
          {/* 海报预览 */}
          <div 
            ref={posterRef}
            className="mx-auto"
            style={{
              width: '750px',
              minHeight: '1334px',
              background: 'linear-gradient(to bottom, #1a0b00 0%, #000 100%)',
              position: 'relative',
              overflow: 'hidden',
              boxShadow: '0 0 50px rgba(184, 134, 11, 0.3)',
              border: '1px solid #333',
              borderRadius: '8px'
            }}
          >
            {/* 背景光效 */}
            <div style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              background: 'radial-gradient(circle at 50% 10%, rgba(255, 215, 0, 0.15) 0%, transparent 60%)',
              pointerEvents: 'none'
            }} />
            
            {/* 网格背景 */}
            <div style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              backgroundImage: `
                linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
                linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px)
              `,
              backgroundSize: '20px 20px',
              pointerEvents: 'none'
            }} />

            {/* 标题区域 */}
            <div style={{ textAlign: 'center', paddingTop: '40px', position: 'relative', zIndex: 2 }}>
              <h1 style={{
                fontSize: '36px',
                color: '#fff',
                textShadow: '0 0 10px rgba(255, 215, 0, 0.8)',
                margin: 0,
                fontFamily: 'sans-serif'
              }}>
                {companyName}
              </h1>
              <h2 style={{
                fontSize: '42px',
                fontWeight: 900,
                margin: '8px 0 0 0',
                letterSpacing: '2px',
                background: 'linear-gradient(to bottom, #fff 10%, #ffd700 40%, #b8860b 60%, #5e2c04 100%)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
                backgroundClip: 'text'
              }}>
                {eventTitle}
              </h2>
              <p style={{
                fontSize: '16px',
                color: '#ffd700',
                margin: '8px 0',
                opacity: 0.8
              }}>
                {date}
              </p>
            </div>

            {/* 前三名领奖台 */}
            {topThree.length > 0 && (
              <div style={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'flex-end',
                padding: '30px 20px',
                position: 'relative',
                zIndex: 2
              }}>
                {/* 第二名 */}
                {topThree.length >= 2 && (
                  <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    margin: '0 15px',
                    textAlign: 'center'
                  }}>
                    <div style={{
                      width: '80px',
                      height: '80px',
                      borderRadius: '50%',
                      background: 'linear-gradient(180deg, #c0c0c0, #808080)',
                      padding: '3px',
                      boxShadow: '0 0 20px rgba(192, 192, 192, 0.3)'
                    }}>
                      <div style={{
                        width: '100%',
                        height: '100%',
                        borderRadius: '50%',
                        background: '#333',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#c0c0c0',
                        fontSize: '24px',
                        fontWeight: 'bold'
                      }}>
                        🥈
                      </div>
                    </div>
                    <div style={{ marginTop: '8px', color: '#fff' }}>
                      <span style={{ fontSize: '16px', fontWeight: 'bold', display: 'block' }}>
                        {topThree[1].anchor_name || topThree[1].anchor_id}
                      </span>
                      <span style={{
                        fontSize: '12px',
                        color: '#c0c0c0',
                        background: 'rgba(0,0,0,0.6)',
                        padding: '2px 8px',
                        borderRadius: '10px',
                        display: 'inline-block',
                        marginTop: '4px'
                      }}>
                        {getGrade(topThree[1].wave_value)}
                      </span>
                    </div>
                  </div>
                )}

                {/* 第一名 */}
                {topThree.length >= 1 && (
                  <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    margin: '0 15px',
                    textAlign: 'center'
                  }}>
                    <div style={{
                      fontSize: '28px',
                      marginBottom: '-10px',
                      textShadow: '0 0 10px #fff'
                    }}>
                      👑
                    </div>
                    <div style={{
                      width: '100px',
                      height: '100px',
                      borderRadius: '50%',
                      background: 'linear-gradient(180deg, #ffd700, #b8860b)',
                      padding: '4px',
                      boxShadow: '0 0 30px rgba(255, 215, 0, 0.5)',
                      border: '2px solid #fff'
                    }}>
                      <div style={{
                        width: '100%',
                        height: '100%',
                        borderRadius: '50%',
                        background: '#333',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#ffd700',
                        fontSize: '32px',
                        fontWeight: 'bold'
                      }}>
                        🥇
                      </div>
                    </div>
                    <div style={{ marginTop: '10px', color: '#fff' }}>
                      <span style={{ fontSize: '20px', fontWeight: 'bold', display: 'block', color: '#ffd700' }}>
                        {topThree[0].anchor_name || topThree[0].anchor_id}
                      </span>
                      <span style={{
                        fontSize: '14px',
                        color: '#ffd700',
                        background: 'rgba(0,0,0,0.6)',
                        padding: '2px 10px',
                        borderRadius: '10px',
                        display: 'inline-block',
                        marginTop: '4px',
                        border: '1px solid #b8860b'
                      }}>
                        {getGrade(topThree[0].wave_value)}
                      </span>
                    </div>
                  </div>
                )}

                {/* 第三名 */}
                {topThree.length >= 3 && (
                  <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    margin: '0 15px',
                    textAlign: 'center'
                  }}>
                    <div style={{
                      width: '80px',
                      height: '80px',
                      borderRadius: '50%',
                      background: 'linear-gradient(180deg, #cd7f32, #8b4513)',
                      padding: '3px',
                      boxShadow: '0 0 20px rgba(205, 127, 50, 0.3)'
                    }}>
                      <div style={{
                        width: '100%',
                        height: '100%',
                        borderRadius: '50%',
                        background: '#333',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: '#cd7f32',
                        fontSize: '24px',
                        fontWeight: 'bold'
                      }}>
                        🥉
                      </div>
                    </div>
                    <div style={{ marginTop: '8px', color: '#fff' }}>
                      <span style={{ fontSize: '16px', fontWeight: 'bold', display: 'block' }}>
                        {topThree[2].anchor_name || topThree[2].anchor_id}
                      </span>
                      <span style={{
                        fontSize: '12px',
                        color: '#cd7f32',
                        background: 'rgba(0,0,0,0.6)',
                        padding: '2px 8px',
                        borderRadius: '10px',
                        display: 'inline-block',
                        marginTop: '4px'
                      }}>
                        {getGrade(topThree[2].wave_value)}
                      </span>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* 其余排名列表 */}
            {restList.length > 0 && (
              <div style={{
                padding: '0 30px',
                position: 'relative',
                zIndex: 2
              }}>
                {restList.map((item, index) => (
                  <div key={item.id || index} style={{
                    display: 'flex',
                    alignItems: 'center',
                    height: '44px',
                    background: 'linear-gradient(90deg, rgba(0,0,0,0.8) 0%, rgba(50,30,0,0.6) 50%, rgba(0,0,0,0) 100%)',
                    borderBottom: '1px solid rgba(184, 134, 11, 0.5)',
                    borderLeft: '2px solid #b8860b',
                    padding: '0 15px',
                    marginBottom: '8px',
                    borderRadius: '0 4px 4px 0'
                  }}>
                    <span style={{
                      fontSize: '22px',
                      fontWeight: 900,
                      fontStyle: 'italic',
                      width: '40px',
                      color: '#fff',
                      textShadow: '2px 2px 0 #b8860b'
                    }}>
                      {index + 4}
                    </span>
                    <span style={{
                      flex: 1,
                      color: '#fff',
                      fontSize: '15px',
                      fontWeight: 'bold'
                    }}>
                      {item.anchor_name || item.anchor_id}
                    </span>
                    <span style={{
                      fontSize: '13px',
                      color: '#ffd700',
                      marginRight: '15px',
                      opacity: 0.9
                    }}>
                      {formatWave(item.wave_value)}
                    </span>
                    <span style={{
                      fontSize: '13px',
                      color: '#ffd700',
                      background: 'rgba(0,0,0,0.6)',
                      padding: '2px 10px',
                      borderRadius: '10px',
                      border: '1px solid rgba(184, 134, 11, 0.5)'
                    }}>
                      {getGrade(item.wave_value)}
                    </span>
                  </div>
                ))}
              </div>
            )}

            {/* 底部光效 */}
            <div style={{
              position: 'absolute',
              bottom: 0,
              left: 0,
              right: 0,
              height: '150px',
              background: 'linear-gradient(to top, rgba(184, 134, 11, 0.2), transparent)',
              pointerEvents: 'none'
            }} />
          </div>
        </div>

        <div className="px-6 py-4 border-t border-slate-700 flex gap-3 shrink-0">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-2 border border-slate-600 rounded-lg text-text-muted hover:bg-slate-700 transition-colors"
          >
            取消
          </button>
          <button
            onClick={handleExport}
            disabled={exporting || data.length === 0}
            className="flex-1 px-4 py-2 bg-accent-gold hover:bg-yellow-500 disabled:opacity-50 rounded-lg text-black font-medium transition-colors flex items-center justify-center gap-2"
          >
            {exporting ? (
              <>
                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                导出中...
              </>
            ) : (
              <>
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
                导出海报
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}

export default PosterExport
