const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({ region: "europe-west1" });

/**
 * 1. Yeni bir teklif (Offer) eklendiğinde çalışır.
 * İlanın (Job) sahibini bulur ve ona bildirim atar.
 */
exports.onNewOfferCreated = onDocumentCreated("offers/{offerId}", async (event) => {
    const offer = event.data.data();
    if (!offer) return;

    const jobId = offer.jobId;
    const offeredByUserName = offer.offeredByUserName;
    const price = offer.price;

    try {
        // İlan detaylarını çek (ilan sahibinin kim olduğunu bulmak için)
        const jobSnapshot = await admin.firestore().collection("jobs").doc(jobId).get();
        if (!jobSnapshot.exists) return;

        const job = jobSnapshot.data();
        const jobOwnerId = job.postedByUserId;

        // İlan sahibinin bilgilerini (FCM Token dahil) çek
        const userSnapshot = await admin.firestore().collection("users").doc(jobOwnerId).get();
        if (!userSnapshot.exists) return;

        const user = userSnapshot.data();
        const fcmToken = user.fcmToken;

        // Token yoksa bildirim atamayız
        if (!fcmToken) {
            console.log(`Kullanıcı (${jobOwnerId}) için FCM Token bulunamadı.`);
            return;
        }

        // Bildirim paketi hazırla
        const payload = {
            token: fcmToken,
            notification: {
                title: "Yeni Bir Teklif Var! 🎉",
                body: `${offeredByUserName}, "${job.title}" ilanına ${price} TL teklif verdi.`,
            },
            data: {
                click_action: "FLUTTER_NOTIFICATION_CLICK",
                jobId: jobId
            }
        };

        // Bildirimi gönder
        await admin.messaging().send(payload);
        console.log(`Bildirim başarıyla gönderildi: ${jobOwnerId}`);

    } catch (error) {
        console.error("Bildirim gönderilirken hata oluştu:", error);
    }
});

/**
 * 2. Teklif durumu ACCEPTED (Kabul Edildi) olduğunda çalışır.
 * Teklifi veren ustayı bulur ve ona "Teklifin kabul edildi" bildirimi atar.
 */
exports.onOfferAccepted = onDocumentUpdated("offers/{offerId}", async (event) => {
    const beforeOffer = event.data.before.data();
    const afterOffer = event.data.after.data();

    if (!beforeOffer || !afterOffer) return;

    // Sadece status "PENDING" -> "ACCEPTED" olduysa çalış
    if (beforeOffer.status !== "PENDING" || afterOffer.status !== "ACCEPTED") {
        return;
    }

    const workerId = afterOffer.offeredByUserId;
    const jobId = afterOffer.jobId;

    try {
        // İlanın başlığını çek
        const jobSnapshot = await admin.firestore().collection("jobs").doc(jobId).get();
        const jobTitle = jobSnapshot.exists ? jobSnapshot.data().title : "İlan";

        // Ustanın bilgilerini (FCM Token dahil) çek
        const workerSnapshot = await admin.firestore().collection("users").doc(workerId).get();
        if (!workerSnapshot.exists) return;

        const worker = workerSnapshot.data();
        const fcmToken = worker.fcmToken;

        if (!fcmToken) {
            console.log(`Usta (${workerId}) için FCM Token bulunamadı.`);
            return;
        }

        // Bildirim paketi hazırla
        const payload = {
            token: fcmToken,
            notification: {
                title: "Tebrikler! Teklifiniz Kabul Edildi 🤝",
                body: `"${jobTitle}" işi için teklifiniz onaylandı. Müşteri ile sohbete başlayabilirsiniz.`,
            },
            data: {
                click_action: "FLUTTER_NOTIFICATION_CLICK",
                jobId: jobId
            }
        };

        // Bildirimi gönder
        await admin.messaging().send(payload);
        console.log(`Kabul bildirimi başarıyla gönderildi: ${workerId}`);

    } catch (error) {
        console.error("Kabul bildirimi gönderilirken hata oluştu:", error);
    }
});
