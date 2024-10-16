Reveal.addEventListener('ready', function() {
  //console.log("reveal loaded")

  const startButtons = document.getElementsByClassName('start-share-screen')
  for (const button of startButtons) {
    button.addEventListener('click', (evt) => shareScreen(evt, 'start'))
  }

  const stopButtons = document.getElementsByClassName('stop-share-screen')
  for (const button of stopButtons) {
    button.addEventListener('click', (evt) => shareScreen(evt, 'stop'))
  }
})

Reveal.addEventListener('slidechanged', function(event) {
  const video = event.currentSlide.querySelector('video')
  if (video && video.paused && !video.ended) {
    console.log("forcing play of video:", video.id)
    video.play()
  }
})

async function shareScreen(evt, action) {
  const targetIds = evt.target.dataset.targets.split(/[ ,]/)
  const targets = targetIds.map(t => document.getElementById(t))
  //console.log("targets:", targets)
  if (action === 'start') {
    try {
      // Ask the user to select a window or screen to capture
      const mediaStream = await navigator.mediaDevices.getDisplayMedia({
        video: true // Request video capture
      })

      // Set the source of both video elements to the same MediaStream
      for (const target of targets) {
        target.srcObject = mediaStream
      }
      console.log('Screen sharing started on: ' + targetIds)

      // Listen for the stop event to handle when the stream is stopped externally
      mediaStream.getVideoTracks()[0].onended = () => {
        for (const target of targets) {
          target.srcObject = null
        }
        console.log('Screen sharing stopped on: ' + targetIds)
      }
    } catch (error) {
      console.error('Error sharing the screen:', error)
    }
  } else if (action === 'stop') {
    for (const target of targets) {
      if (target.srcObject) {
        target.srcObject.getTracks().forEach(track => track.stop())
        target.srcObject = null
      }
    }
    console.log('Screen sharing stopped on: ' + targetIds)
  }
}
