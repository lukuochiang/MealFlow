<script lang="ts" setup>
import { propTypes } from '@/utils/propTypes'
import Iconify from '@purge-icons/generated'
import { iconExists } from '@iconify/iconify'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { useDesign } from '@/hooks/web/useDesign'

defineOptions({ name: 'Icon' })

const { getPrefixCls } = useDesign()

const prefixCls = getPrefixCls('icon')

const props = defineProps({
  // icon name
  icon: propTypes.string,
  // icon color
  color: propTypes.string,
  // icon size
  size: propTypes.number.def(16),
  // icon svg class
  svgClass: propTypes.string.def('')
})

const elRef = ref<ElRef>(null)

const isLocal = computed(() => (props.icon || '').startsWith('svg-icon:'))

/**
 * system_menu 里大量一级菜单仍是 vue-element-admin 老式图标名
 * （system / monitor / people / rate …），项目已无对应的本地 SVG 资源，
 * 这里映射到语义相近的 Element Plus 图标；未列出的名字会先尝试同名映射，
 * 仍匹配不到时统一用 Menu 图标兜底，保证折叠后每项都有可见图标。
 */
const LEGACY_ICON_MAP: Record<string, string> = {
  build: 'Tools',
  button: 'Operation',
  cascader: 'Grid',
  chart: 'TrendCharts',
  code: 'EditPen',
  config: 'Setting',
  dashboard: 'HomeFilled',
  dict: 'Collection',
  drag: 'Rank',
  druid: 'DataLine',
  education: 'Reading',
  email: 'Message',
  example: 'Files',
  eye: 'View',
  'eye-open': 'View',
  form: 'Document',
  job: 'Timer',
  log: 'Document',
  logininfor: 'Document',
  merchant: 'Shop',
  nested: 'FolderOpened',
  online: 'Connection',
  order: 'Tickets',
  pay: 'Wallet',
  people: 'UserFilled',
  peoples: 'User',
  post: 'Postcard',
  radio: 'CircleCheck',
  rate: 'Star',
  redis: 'Coin',
  row: 'Sort',
  server: 'Monitor',
  skill: 'MagicStick',
  swagger: 'Document',
  system: 'Setting',
  table: 'Grid',
  textarea: 'Document',
  time: 'Clock',
  'time-range': 'Clock',
  tool: 'Tools',
  tree: 'Grid',
  'tree-table': 'Grid',
  validCode: 'Key',
  wechat: 'ChatDotRound',
  '#': 'MoreFilled'
}

const toPascalCase = (name: string) =>
  name
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .split('-')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join('')

/**
 * 后台菜单 icon 由接口运行时下发，而 vite-plugin-purge-icons 只扫描前端源码里
 * 出现过的图标名来打包数据，动态图标名拿不到数据 → Iconify.renderSVG 返回空 → 菜单空白。
 * 折叠态只剩图标，因此这个问题在折叠后暴露得最明显。
 *
 * 这里用本地 Element Plus 图标组件兜底：
 * - ep:xxx                  → 按名映射，后台新增任意 ep 图标自动生效
 * - 无集合前缀的老式名       → 别名表 / 同名映射（若 Iconify 已有数据则保持原链路）
 * - 其余带冒号的 iconify 名 → 保持 Iconify 原链路，不影响已打包的 fa/mdi/carbon 等图标
 */
const epIconComponent = computed(() => {
  const icon = props.icon || ''
  if (!icon || unref(isLocal)) return null

  let componentName = ''
  if (icon.startsWith('ep:')) {
    componentName = toPascalCase(icon.slice(3))
  } else if (!icon.includes(':')) {
    // 源码里硬编码且已打包进 Iconify 数据的纯名字，仍走原渲染链路
    if (iconExists(icon)) return null
    componentName = LEGACY_ICON_MAP[icon] || toPascalCase(icon)
  } else {
    return null
  }

  return (ElementPlusIcons as Record<string, any>)[componentName] ?? ElementPlusIcons.Menu
})

const symbolId = computed(() => {
  return unref(isLocal) ? `#icon-${props.icon.split('svg-icon:')[1]}` : props.icon
})

const getIconifyStyle = computed(() => {
  const { color, size } = props
  return {
    fontSize: `${size}px`,
    height: '1em',
    color
  }
})

const getSvgClass = computed(() => {
  const { svgClass } = props
  return `iconify ${svgClass}`
})

const updateIcon = async (icon: string) => {
  if (unref(isLocal) || unref(epIconComponent)) return

  const el = unref(elRef)
  if (!el) return

  await nextTick()

  if (!icon) return

  const svg = Iconify.renderSVG(icon, {})
  if (svg) {
    el.textContent = ''
    el.appendChild(svg)
  } else {
    const span = document.createElement('span')
    span.className = 'iconify'
    span.dataset.icon = icon
    el.textContent = ''
    el.appendChild(span)
  }
}

watch(
  () => props.icon,
  (icon: string) => {
    updateIcon(icon)
  }
)

// watch 只在 icon 变化时触发；首次挂载（尤其是接口返回后动态渲染的后台菜单）
// 必须主动渲染一次，否则图标不会被替换成 SVG
onMounted(() => {
  updateIcon(props.icon)
})
</script>

<template>
  <ElIcon :class="prefixCls" :color="color" :size="size">
    <svg v-if="isLocal" :class="getSvgClass" aria-hidden="true">
      <use :xlink:href="symbolId" />
    </svg>

    <component :is="epIconComponent" v-else-if="epIconComponent" />

    <span v-else ref="elRef" :class="$attrs.class" :style="getIconifyStyle">
      <span :class="getSvgClass" :data-icon="symbolId"></span>
    </span>
  </ElIcon>
</template>
