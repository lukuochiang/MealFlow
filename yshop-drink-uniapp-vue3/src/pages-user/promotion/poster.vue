<template>
	<layout>
		<uv-navbar :fixed="false" :title="title" left-arrow @leftClick="$onClickLeft" />
		<view class="poster-page">
			<view class="poster-card bg-white">
				<view class="poster-title">邀请好友，赚取佣金</view>
				<view class="poster-sub">好友通过你的链接下单，你可获得一级/二级佣金</view>
				<view class="poster-link">
					<text selectable>{{ inviteLink }}</text>
				</view>
				<view class="poster-actions">
					<uv-button type="primary" @click="copyLink">复制邀请链接</uv-button>
				</view>
				<view class="poster-tip">小程序/APP 点击右上角「···」转发，链接已自动携带你的推广ID；H5 可将此链接发送给好友</view>
			</view>
		</view>
	</layout>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
import { useMainStore } from '@/store/store'
import { storeToRefs } from 'pinia'

const main = useMainStore()
const { member, isLogin } = storeToRefs(main)
const title = ref('分享海报')
const uid = ref('')
const inviteLink = ref('')

onLoad(() => {
	uid.value = isLogin.value ? (member.value.id || '') : ''
	// 分享落地页携带推广人ID；H5 下可直接拼接（如需完整域名可在后端下发）
	inviteLink.value = '/pages/index/index?spreadUid=' + uid.value
})

const copyLink = () => {
	uni.setClipboardData({ data: inviteLink.value })
}

// 微信小程序/APP 转发携带推广参数
onShareAppMessage(() => {
	return {
		title: '邀请你来越喝一杯，下单返佣金',
		path: '/pages/index/index?spreadUid=' + uid.value
	}
})
</script>

<style lang="scss" scoped>
.poster-page { padding: 24rpx; }
.poster-card { border-radius: 16rpx; padding: 40rpx; text-align: center; }
.poster-title { font-size: 36rpx; font-weight: bold; }
.poster-sub { color: #999; font-size: 24rpx; margin: 12rpx 0 24rpx; }
.poster-link { background: #f5f5f5; padding: 16rpx; border-radius: 8rpx; word-break: break-all; margin-bottom: 24rpx; font-size: 24rpx; }
.poster-actions { display: flex; justify-content: center; }
.poster-tip { color: #999; font-size: 22rpx; margin-top: 16rpx; }
</style>
