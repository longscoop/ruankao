export interface AdminProfile {
  userId: number
  username: string
  displayName: string
  role: 'ADMIN'
}

export interface AdminLoginResponse extends AdminProfile {
  token: string
}
