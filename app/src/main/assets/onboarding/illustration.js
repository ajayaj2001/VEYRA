const illustration = document.querySelector('.illustration');
const designWidth = illustration.offsetWidth;
const designHeight = illustration.offsetHeight;

function fitIllustration() {
  const { clientWidth, clientHeight } = document.documentElement;
  const scale = Math.min(clientWidth / designWidth, clientHeight / designHeight);
  const x = (clientWidth - designWidth * scale) / 2;
  const y = (clientHeight - designHeight * scale) / 2;
  illustration.style.transform = `translate(${x}px, ${y}px) scale(${scale})`;
}

fitIllustration();
addEventListener('resize', fitIllustration);
