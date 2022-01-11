<template>
  <div>
    <svg-icon icon-class="github" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'RuoYi-Cloud-Plus-Git',
  data() {
    return {
      url: 'https://gitee.com/JavaLionLi/RuoYi-Cloud-Plus'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>
