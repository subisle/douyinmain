import { useState, useCallback } from 'react'

export interface ToastMessage {
  message: string
  type: 'success' | 'error' | 'info'
  duration?: number
}

export function useToast() {
  const [toast, setToast] = useState<ToastMessage | null>(null)

  const showToast = useCallback((message: string, type: 'success' | 'error' | 'info' = 'info', duration = 3000) => {
    setToast({ message, type, duration })
  }, [])

  const showSuccess = useCallback((message: string, duration?: number) => {
    showToast(message, 'success', duration)
  }, [showToast])

  const showError = useCallback((message: string, duration?: number) => {
    showToast(message, 'error', duration)
  }, [showToast])

  const showInfo = useCallback((message: string, duration?: number) => {
    showToast(message, 'info', duration)
  }, [showToast])

  const hideToast = useCallback(() => {
    setToast(null)
  }, [])

  // 错误处理包装器
  const withErrorHandling = useCallback(<T extends any[], R>(
    fn: (...args: T) => Promise<R>,
    errorMessage?: string
  ) => {
    return async (...args: T): Promise<R | null> => {
      try {
        return await fn(...args)
      } catch (error) {
        const message = errorMessage || `操作失败: ${String(error)}`
        showError(message)
        console.error('操作失败:', error)
        return null
      }
    }
  }, [showError])

  return {
    toast,
    showToast,
    showSuccess,
    showError,
    showInfo,
    hideToast,
    withErrorHandling
  }
}