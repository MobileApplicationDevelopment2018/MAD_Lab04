const functions = require('firebase-functions');

const admin = require('firebase-admin');
admin.initializeApp();

exports.onNewMessage = functions.database.ref('/conversations/{cid}/messages/{mid}')
	.onCreate((snapshot, context) => {
		const cid = context.params.cid;
  		const uid = context.auth.uid;
		const rid = snapshot.child('recipient').val();
  		const timestamp = snapshot.child('timestamp').val() * (-1);

  		let promises = []

		promises.push(admin.database().ref('/users/' + rid + '/conversations/active/' + cid + '/unreadMessages')
			.transaction(count => {
				return (count || 0) + 1;
			}));

  		promises.push(admin.database().ref('/users/' + uid + '/conversations/active/' + cid + '/timestamp').set(timestamp));
        promises.push(admin.database().ref('/users/' + rid + '/conversations/active/' + cid + '/timestamp').set(timestamp));

  		return Promise.all(promises);
	});