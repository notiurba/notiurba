export const paginaCriticosAnterior = (pagina, totalPaginas) => Math.max(1, Math.min(totalPaginas, pagina - 1))
export const paginaCriticosSiguiente = (pagina, totalPaginas) => Math.max(1, Math.min(totalPaginas, pagina + 1))
