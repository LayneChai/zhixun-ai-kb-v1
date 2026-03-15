export const getPerms = () => {
  try {
    const raw = localStorage.getItem('perms')
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

export const setPerms = (perms) => {
  const list = Array.isArray(perms) ? perms : []
  localStorage.setItem('perms', JSON.stringify(list))
  localStorage.setItem('perms_loaded', '1')
}

export const hasPerm = (perm) => {
  if (!perm) return true
  const list = getPerms()
  return list.includes(perm)
}

export const hasAnyPerm = (perms) => {
  if (!perms || !perms.length) return true
  const list = getPerms()
  return perms.some(p => list.includes(p))
}
