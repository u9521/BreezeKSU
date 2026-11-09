<script setup lang="ts">
import { useData } from 'vitepress'
import { onBeforeUnmount, onMounted, ref } from 'vue'

// The default theme positions every fixed element (nav, sidebar, local nav, content)
// against --vp-layout-top-height, which is 0 unless something reserves the space.
// The banner measures itself and publishes that height, so nothing is covered.
const root = ref<HTMLElement | null>(null)
const { localeIndex } = useData()

const REPO = 'https://github.com/u9521/BreezeKSU'

const copy = {
    root: {
        label: 'Work in progress',
        body: "This is the BreezeKSU fork's documentation. The text is inherited from upstream KernelSU and has not been rewritten for this fork; some pages describe features this fork has removed.",
        link: 'Fork repository',
    },
    zh_CN: {
        label: '施工中',
        body: '这是 BreezeKSU 分支的文档。正文继承自上游 KernelSU，尚未针对本分支改写；部分页面所描述的功能在本分支已被移除。',
        link: '本分支仓库',
    },
}

const t = () => copy[localeIndex.value as keyof typeof copy] ?? copy.root

let observer: ResizeObserver | undefined

function publish(height: number) {
    document.documentElement.style.setProperty('--vp-layout-top-height', `${height}px`)
}

onMounted(() => {
    if (!root.value) return
    publish(root.value.offsetHeight)
    // contentRect excludes padding and border, so read the border-box height off the
    // element itself: the banner's padding is part of what the nav must clear.
    observer = new ResizeObserver(() => {
        if (root.value) publish(root.value.offsetHeight)
    })
    observer.observe(root.value)
})

onBeforeUnmount(() => {
    observer?.disconnect()
    observer = undefined
    document.documentElement.style.removeProperty('--vp-layout-top-height')
})
</script>

<template>
    <div ref="root" class="WipBanner">
        <p class="WipBanner-text">
            <span class="WipBanner-label">{{ t().label }}</span>
            <span class="WipBanner-body">{{ t().body }}</span>
            <a class="WipBanner-link" :href="REPO" target="_blank" rel="noreferrer">{{ t().link }}</a>
        </p>
    </div>
</template>

<style scoped>
.WipBanner {
    position: fixed;
    top: 0;
    right: 0;
    left: 0;
    z-index: calc(var(--vp-z-index-nav) + 1);
    padding: 8px 24px;
    border-bottom: 1px solid var(--vp-c-warning-1);
    background-color: var(--vp-c-warning-soft);
    color: var(--vp-c-text-1);
}

.WipBanner-text {
    margin: 0;
    font-size: 12px;
    font-weight: 500;
    line-height: 18px;
    text-align: center;
    text-wrap: balance;
}

.WipBanner-label {
    margin-right: 8px;
    padding: 1px 6px;
    border-radius: 4px;
    /* --vp-c-warning-1 is dark brown in light mode and light amber in dark mode,
       so the label's own text colour has to flip with it — a fixed white would
       drop to roughly 2:1 contrast on the dark theme. */
    background-color: var(--vp-c-warning-1);
    color: var(--vp-c-bg);
    font-size: 11px;
    letter-spacing: 0.02em;
    text-transform: uppercase;
    white-space: nowrap;
}

.WipBanner-body {
    color: var(--vp-c-text-2);
}

.WipBanner-link {
    margin-left: 8px;
    color: var(--vp-c-brand-1);
    text-decoration: underline;
    text-underline-offset: 2px;
    white-space: nowrap;
}

.WipBanner-link:hover {
    color: var(--vp-c-brand-2);
}
</style>
