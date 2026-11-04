import DefaultTheme from 'vitepress/theme'
import { h } from 'vue'
import WipBanner from './WipBanner.vue'

// A one-file theme extension: the default theme with a site-wide notice pinned to
// the top. It exists because the docs here are inherited from upstream KernelSU and
// describe features this fork has removed, which a reader cannot infer from the text.
export default {
    extends: DefaultTheme,
    Layout() {
        return h(DefaultTheme.Layout, null, {
            'layout-top': () => h(WipBanner),
        })
    },
}
