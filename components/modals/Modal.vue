<template>
  <div ref="wrapper" class="modal modal-bg w-full h-full max-h-screen fixed top-0 left-0 bg-primary bg-opacity-75 flex items-center justify-center z-50 opacity-0">
    <div class="absolute top-0 left-0 w-full h-40 bg-gradient-to-b from-black to-transparent opacity-90 pointer-events-none" />

    <div class="absolute z-40 top-4 right-4 h-12 w-12 flex items-center justify-center cursor-pointer text-white hover:text-gray-300" @click="show = false">
      <span class="material-icons text-4xl">close</span>
    </div>
    <slot name="outer" />
    <div ref="content" style="max-width: 90%; min-height: 200px" class="relative text-white max-h-screen" :style="{ height: modalHeight, width: modalWidth }" v-click-outside="clickBg">
      <slot />
    </div>
  </div>
</template>

<script>
export default {
  props: {
    value: Boolean,
    processing: Boolean,
    persistent: {
      type: Boolean,
      default: true
    },
    width: {
      type: [String, Number],
      default: 500
    },
    height: {
      type: [String, Number],
      default: 'unset'
    }
  },
  data() {
    return {
      el: null,
      content: null
    }
  },
  watch: {
    show(newVal) {
      if (newVal) {
        this.setShow()
      } else {
        this.setHide()
      }
    }
  },
  computed: {
    show: {
      get() {
        return this.value
      },
      set(val) {
        this.$emit('input', val)
      }
    },
    modalHeight() {
      if (typeof this.height === 'string') {
        return this.height
      } else {
        return this.height + 'px'
      }
    },
    modalWidth() {
      return typeof this.width === 'string' ? this.width : this.width + 'px'
    }
  },
  methods: {
    clickBg(vm, ev) {
      if (this.processing && this.persistent) return
      if (vm.srcElement.classList.contains('modal-bg')) {
        this.show = false
      }
    },
    setShow() {
      document.body.appendChild(this.el)
      setTimeout(() => {
        this.content.style.transform = 'scale(1)'
      }, 10)
      document.documentElement.classList.add('modal-open')
    },
    setHide() {
      this.content.style.transform = 'scale(0)'
      this.el.remove()
      document.documentElement.classList.remove('modal-open')
    }
  },
  mounted() {
    this.el = this.$refs.wrapper
    this.content = this.$refs.content
    this.content.style.transform = 'scale(0)'
    this.content.style.transition = 'transform 0.25s cubic-bezier(0.16, 1, 0.3, 1)'
    this.el.style.opacity = 1
    this.el.remove()
  }
}
</script>