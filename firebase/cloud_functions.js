const functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp();

exports.onNewMessage = functions.database.ref('/conversations/{cid}/messages/{mid}')
	.onCreate((snapshot, context) => {
		const cid = context.params.cid;
	    const uid = snapshot.child('recipient').val();

		return admin.database().ref('/users/' + uid + '/conversations/active/' + cid)
			.transaction(count => {
				return (count || 0) + 1;
			});
	});
