<template>
  <div class="flex h-full px-1 overflow-hidden">
    <div class="overflow-hidden bg-primary rounded" style="height: 50px; width: 40px">
      <covers-author-image :author="author" />
    </div>
    <div class="flex-grow px-2 authorSearchCardContent h-full">
      <p class="truncate text-sm">{{ name }}</p>
    </div>
  </div>
  <!-- <div class="flex h-full px-1 overflow-hidden shadow-sm">
    <div style="max-height: 48px; max-width: 48px" class="w-12 h-12 bg-primary overflow-hidden rounded">
      <svg width="140%" height="140%" style="margin-left: -20%; margin-top: -20%; opacity: 0.6" viewBox="0 0 177 266" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path fill="white" d="M40.7156 165.47C10.2694 150.865 -31.5407 148.629 -38.0532 155.529L63.3191 204.159L76.9443 190.899C66.828 181.394 54.006 171.846 40.7156 165.47Z" stroke="white" stroke-width="4" transform="translate(-2 -1)" />
        <path d="M-38.0532 155.529C-31.5407 148.629 10.2694 150.865 40.7156 165.47C54.006 171.846 66.828 181.394 76.9443 190.899L95.0391 173.37C80.6681 159.403 64.7526 149.155 51.5747 142.834C21.3549 128.337 -46.2471 114.563 -60.6897 144.67L-71.5489 167.307L44.5864 223.019L63.3191 204.159L-38.0532 155.529Z" fill="white" />
        <path
          d="M105.87 29.6508C80.857 17.6515 50.8784 28.1923 38.879 53.2056C26.8797 78.219 37.4205 108.198 62.4338 120.197C87.4472 132.196 117.426 121.656 129.425 96.6422C141.425 71.6288 130.884 41.6502 105.87 29.6508ZM106.789 85.783C112.761 73.3329 107.461 58.2599 95.0112 52.2874C82.5611 46.3148 67.4881 51.6147 61.5156 64.0648C55.543 76.5149 60.8429 91.5879 73.293 97.5604C85.7431 103.533 100.816 98.2331 106.789 85.783Z"
          fill="white"
        />
        <path
          d="M151.336 159.01L159.048 166.762L82.7048 242.703L74.973 242.683L74.9934 234.951L151.336 159.01ZM181.725 108.497C179.624 108.491 177.436 109.326 175.835 110.918L160.415 126.257L191.848 157.856L207.268 142.517C210.554 139.248 210.568 133.954 207.299 130.667L187.685 110.95C186.009 109.264 183.91 108.502 181.725 108.497ZM151.399 135.226L58.2034 227.931L58.1203 259.447L89.6359 259.53L182.831 166.825L151.399 135.226Z"
          fill="white"
        />
        <path d="M151.336 159.01L159.048 166.762L82.7048 242.703L74.973 242.683L74.9934 234.951L151.336 159.01Z" fill="white" stroke="white" stroke-width="10px" />
      </svg>
    </div>
    <div class="flex-grow px-2 authorSearchCardContent h-full">
      <p class="truncate text-sm">{{ author }}</p>
    </div>
  </div> -->
</template>

<script>
export default {
  props: {
    author: {
      type: Object,
      default: () => {}
    }
  },
  data() {
    return {}
  },
  computed: {
    name() {
      return this.author.name
    }
  },
  methods: {},
  mounted() {}
}
</script>

<style>
.authorSearchCardContent {
  width: calc(100% - 50px);
  display: flex;
  flex-direction: column;
  justify-content: center;
}
</style>