<template>
	<layout>
		<uv-navbar :fixed="false" :title="title" left-arrow @leftClick="$onClickLeft" />
		<view class="commission-page">
			<view class="commission-card bg-white">
				<view class="label">可提现佣金(元)</view>
				<view class="amount">{{ brokerage }}</view>
				<view class="sub">下级人数 {{ spreadCount }} · {{ isPromoter ? '推广员' : '普通用户' }}</view>
				<view class="actions">
					<uv-button type="primary" size="small" @click="goPoster">生成分享海报</uv-button>
				</view>
			</view>

			<view class="commission-block bg-white">
				<view class="block-title">申请提现</view>
				<view class="withdraw-form">
					<uv-input v-model="amount" type="number" placeholder="请输入提现金额（低于门槛不可提）" />
					<uv-button type="error" size="small" @click="apply">提交申请</uv-button>
				</view>
			</view>

			<view class="commission-block bg-white">
				<view class="block-title">提现记录</view>
				<view v-if="list.length === 0" class="empty">暂无提现记录</view>
				<view v-for="(item,idx) in list" :key="idx" class="withdraw-item">
					<view class="wi-top">
						<text>提现 {{ item.price }} 元</text>
						<text :class="['wi-status', statusClass(item.status)]">{{ statusText(item.status) }}</text>
					</view>
					<view class="wi-time" v-if="item.createTime">{{ item.createTime }}</view>
				</view>
			</view>
		</view>
	</layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useMainStore } from '@/store/store'
import { storeToRefs } from 'pinia'
import { navigateTo } from '@/utils/route'
import { userGetUserInfo, brokerageWithdrawList, brokerageWithdrawApply } from '@/api/user'

const main = useMainStore()
const { member, isLogin } = storeToRefs(main)
const title = ref('佣金中心')
const list = ref([])
const amount = ref('')

const brokerage = computed(() => isLogin.value ? (member.value.brokeragePrice || 0) : 0)
const spreadCount = computed(() => isLogin.value ? (member.value.spreadCount || 0) : 0)
const isPromoter = computed(() => isLogin.value && member.value.isPromoter === 1)

const statusText = (s) => s === 1 ? '已通过' : (s === -1 ? '已拒绝' : '待审核')
const statusClass = (s) => s === 1 ? 'ok' : (s === -1 ? 'no' : 'wait')

const goPoster = () => navigateTo('/pages-user/promotion/poster')

const loadData = async () => {
	if (!isLogin.value) return
	let info = await userGetUserInfo()
	if (info) main.SET_MEMBER(info)
	let data = await brokerageWithdrawList()
	if (data) list.value = data
}

const apply = async () => {
	if (!amount.value || Number(amount.value) <= 0) {
		uni.showToast({ title: '请输入提现金额', icon: 'none' })
		return
	}
	await brokerageWithdrawApply({ price: Number(amount.value) })
	uni.showToast({ title: '已提交，等待审核', icon: 'success' })
	amount.value = ''
	loadData()
}

onShow(() => { loadData() })
</script>

<style lang="scss" scoped>
.commission-page { padding: 24rpx; }
.commission-card { border-radius: 16rpx; padding: 40rpx; text-align: center; }
.commission-card .label { color: #999; font-size: 26rpx; }
.commission-card .amount { font-size: 64rpx; font-weight: bold; color: #e54d42; margin: 12rpx 0; }
.commission-card .sub { color: #999; font-size: 24rpx; margin-bottom: 24rpx; }
.commission-card .actions { display: flex; justify-content: center; gap: 24rpx; }
.commission-block { border-radius: 16rpx; margin-top: 24rpx; padding: 24rpx; }
.block-title { font-weight: bold; margin-bottom: 16rpx; }
.withdraw-form { display: flex; gap: 16rpx; align-items: center; }
.withdraw-item { padding: 16rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.wi-top { display: flex; justify-content: space-between; }
.wi-time { color: #999; font-size: 22rpx; margin-top: 6rpx; }
.wi-status.ok { color: #07c160; }
.wi-status.no { color: #999; }
.wi-status.wait { color: #ff9900; }
.empty { color: #999; text-align: center; padding: 40rpx 0; }
</style>
