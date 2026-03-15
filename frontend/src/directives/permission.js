import { hasAnyPerm } from '../utils/permission'

const check = (el, binding) => {
  const value = binding.value
  const perms = Array.isArray(value) ? value : [value]
  const loaded = localStorage.getItem('perms_loaded') === '1'
  if (!loaded) return
  if (!hasAnyPerm(perms)) {
    el.parentNode && el.parentNode.removeChild(el)
  }
}

export default {
  mounted(el, binding) {
    check(el, binding)
  },
  updated(el, binding) {
    check(el, binding)
  }
}
