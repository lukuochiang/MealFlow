import api from './api'

/**
 * 基本信息 
 */
export function userGetUserInfo(data) {
  return api.get('/member/user/get-info', data, { login: true })
}

/**
 * 获取菜单
 */
export function mineService(data) {
  return api.get('/service/list', data, { login: true })
}

/**
 * 获取内容 
 */
export function mineServiceContent(data) {
  return api.get('/service/content', data, { login: true })
}


/**
 * save 
 */
export function userEdit(data) {
  return api.post('/member/user/update-nickname', data, { login: true })
}


/**
 * balanceGetBillList 
 */
export function balanceGetBillList(data) {
  return api.get('/member/user/getBill', data, { login: true })
}




/**
 * 充值列表 
 */
export function balanceGetMoneyList(data) {
  return api.get('/recharge/getMoneyList', data, { login: true })
}

/**
 * 充值 
 */
export function balanceRecharge(data) {
  return api.post('/member/user/recharge', data, { login: true })
}

/**
 * 绑定推广人（分享注册携带的推荐人）
 */
export function brokerageBindSpread(data) {
  return api.post('/member/user/bind-spread', data, { login: true })
}

/**
 * 申请佣金提现
 */
export function brokerageWithdrawApply(data) {
  return api.post('/member/user/brokerage/withdraw/apply', data, { login: true })
}

/**
 * 我的佣金提现记录
 */
export function brokerageWithdrawList(data) {
  return api.get('/member/user/brokerage/withdraw/list', data, { login: true })
}