var config = {
	urlNewSession: 'https://{{ pillar['sslhost'] }}/sessions/new',
	urlActiveSession: 'https://{{ pillar['sslhost'] }}/sessions/active',
	urlEndSession: 'https://{{ pillar['sslhost'] }}/sessions/',
	urlUuid: 'https://{{ pillar['sslhost'] }}/sessions/uuid',
	urlUsers: 'https://{{ pillar['sslhost'] }}/users/',
	urlConfirmUser: 'https://{{ pillar['sslhost'] }}/users/confirmation/user',
	urlLoadColors: 'https://{{ pillar['sslhost'] }}/sessions/uuid/colors/stats'
}
