import { useOutletContext } from 'react-router-dom'

// Para que cada paso lea el borrador y las listas de ReportarLayout con un nombre claro.
export const useReporte = () => useOutletContext()
