// This key belongs only to the academic prototype, never to the production API.
const KEY = 'notiurba-evaluacion-v1'
const DRAFT = `${KEY}-draft`
function read(key, fallback) {
  try { return JSON.parse(sessionStorage.getItem(key)) ?? fallback } catch { return fallback }
}
export function loadReports(seeds) {
  const reports = read(KEY, seeds)
  return Array.isArray(reports) && reports.every(r => r && typeof r.id === 'string' && Array.isArray(r.photos)) ? reports : seeds
}
export function saveReports(reports) {
  try { sessionStorage.setItem(KEY, JSON.stringify(reports)); return true } catch { return false }
}
export function loadDraft(fallback) {
  const value = read(DRAFT, fallback)
  return value && typeof value.location === 'string' && typeof value.description === 'string' && Array.isArray(value.photos) ? value : fallback
}
export function saveDraft(draft) {
  try { sessionStorage.setItem(DRAFT, JSON.stringify(draft)) } catch { /* Report storage shows an explicit warning if quota is reached. */ }
}
export function clearDraft() { try { sessionStorage.removeItem(DRAFT) } catch { /* Browser storage may be disabled. */ } }
