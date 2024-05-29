const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendNotification = functions.database.ref("/messages/{messageId}")
    .onCreate((snapshot, context) => {
      const message = snapshot.val();
      const payload = {
        notification: {
          title: `New Message from ${message.senderId}`,
          body: message.message,
          sound: "default"
        },
        data: {
          senderId: message.senderId
        }
      };


      const recipientToken = message.recipientToken;

      return admin.messaging().sendToDevice(recipientToken, payload)
          .then(response => {
                console.log("Successfully sent message:", response);
                return null;
            })
            .catch(error => {
                console.error("Error sending message:", error);
                throw error;
            });
    });
