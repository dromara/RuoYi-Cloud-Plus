<template>
  <div>
    <svg-icon icon-class="question" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'RuoYi-Cloud-Plus-Wiki',
  data() {
    return {
      url: 'https://gitee.com/dromara/RuoYi-Cloud-Plus/wikis'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>
