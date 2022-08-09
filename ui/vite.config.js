import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [vue()],
    //for multi-module maven to bundle
    build: {
        emptyOutDir: true,
        outDir: './target/classes/public'
    }
})

