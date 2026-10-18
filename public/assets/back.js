// The way back for visitors who came from fherrmann.com.
(function () {
  try {
    if (new URL(document.referrer).hostname === 'fherrmann.com') {
      document.getElementById('back-link').classList.add('visible');
    }
  } catch (e) {}
})();
