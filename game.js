/* ==========================================================
   SPELL-MAGE GAME LOGIC & CANVAS ENGINE
   ========================================================== */

document.addEventListener('DOMContentLoaded', () => {
  // Canvas & Context Setup
  const canvas = document.getElementById('gameCanvas');
  const ctx = canvas.getContext('2d');

  // UI Elements
  const hud = document.getElementById('hud');
  const coinsCountEl = document.getElementById('coins-count');
  const scoreCountEl = document.getElementById('score-count');
  const levelCountEl = document.getElementById('level-count');
  const heartsContainer = document.getElementById('hearts-container');
  const wordTargetBar = document.getElementById('word-target-bar');
  const targetWordDisplay = document.getElementById('target-word-display');
  const levelupBanner = document.getElementById('levelup-banner');
  const levelupText = document.getElementById('levelup-text');
  const potionBar = document.getElementById('potion-bar');
  const mainMenuOverlay = document.getElementById('main-menu-overlay');
  const settingsModal = document.getElementById('settings-modal');
  const shopModal = document.getElementById('shop-modal');
  const gameoverModal = document.getElementById('gameover-modal');
  const transparentKeyboard = document.getElementById('transparent-keyboard');
  const shopCoinsVal = document.getElementById('shop-coins-val');
  const shopMsg = document.getElementById('shop-msg');

  // Quick Potion Elements
  const btnUseHeal = document.getElementById('btn-use-heal');
  const btnUseFreeze = document.getElementById('btn-use-freeze');
  const btnUseNuke = document.getElementById('btn-use-nuke');
  const countHealEl = document.getElementById('count-heal');
  const countFreezeEl = document.getElementById('count-freeze');
  const countNukeEl = document.getElementById('count-nuke');

  // Buttons
  const btnPlay = document.getElementById('btn-play');
  const btnSettings = document.getElementById('btn-settings');
  const btnShop = document.getElementById('btn-shop');
  const btnPause = document.getElementById('btn-pause');
  const btnCloseSettings = document.getElementById('btn-close-settings');
  const btnCloseShop = document.getElementById('btn-close-shop');
  const btnRestart = document.getElementById('btn-restart');
  const btnMainMenu = document.getElementById('btn-main-menu');

  // Game Settings & State
  let gameRunning = false;
  let gamePaused = false;
  let hp = 3;
  let maxHp = 3;
  let score = 0;
  let coins = 0;
  let currentLevel = 1;
  let selectedDifficulty = 'lento'; // lento, medio, rapido
  let selectedBgKey = 'bg_mago_estelar.jpg';
  let isFreezeActive = false;
  let freezeTimer = null;

  // Inventory System for Potions
  let inventory = {
    heal: 1,
    freeze: 1,
    nuke: 0
  };

  // Background Image Cache
  const bgImages = {};
  const bgFiles = ['bg_mago_estelar.jpg', 'bg_mago_vortice.jpg', 'bg_mago_varita.jpg', 'bg_mago_galactico.jpg', 'fondomenu.jpeg'];
  bgFiles.forEach(fileName => {
    const img = new Image();
    img.src = 'assets/' + fileName;
    bgImages[fileName] = img;
  });

  // Words bank (Spanish & Fantasy Magic Words)
  const wordBank = [
    'A', 'E', 'I', 'O', 'U', 'SOL', 'LUZ', 'MAR', 'PAZ', 'RUNA', 
    'MAGIA', 'FUEGO', 'HIELO', 'RAYO', 'ALMA', 'ORBE', 'SABIO', 'LIBRO',
    'HECHIZO', 'CRISTAL', 'VARITA', 'ESTRELLA', 'COSMOS', 'ARCANO',
    'CONJURO', 'PODER', 'NUBE', 'TORMENTA', 'ALQUIMIA', 'PULSO'
  ];

  const colorPalettes = [
    { bg: '#9d4edd', border: '#e0aaff', glow: 'rgba(157, 78, 221, 0.8)' },
    { bg: '#2a9d8f', border: '#a8dadc', glow: 'rgba(42, 157, 143, 0.8)' },
    { bg: '#e63946', border: '#ffb703', glow: 'rgba(230, 57, 70, 0.8)' },
    { bg: '#00b4d8', border: '#90e0ef', glow: 'rgba(0, 180, 216, 0.8)' },
    { bg: '#d4af37', border: '#fff7c2', glow: 'rgba(212, 175, 55, 0.8)' },
    { bg: '#f72585', border: '#7209b7', glow: 'rgba(247, 37, 133, 0.8)' }
  ];

  const specialLifeStyle = { bg: '#1b4332', border: '#52b788', glow: 'rgba(82, 183, 136, 0.95)', isSpecial: 'life' };
  const specialBoomStyle = { bg: '#6b0504', border: '#ff758f', glow: 'rgba(255, 117, 143, 0.95)', isSpecial: 'boom' };

  let blocks = [];
  let trailParticles = [];
  let sparkParticles = [];
  let targetBlock = null;

  let spawnTimer = 0;

  const diffConfigs = {
    lento: { baseSpeed: 1.0, spawnInterval: 140, coinMult: 1.0 },
    medio: { baseSpeed: 1.8, spawnInterval: 100, coinMult: 1.5 },
    rapido: { baseSpeed: 2.8, spawnInterval: 65, coinMult: 2.5 }
  };

  function resizeCanvas() {
    const container = document.getElementById('game-container');
    canvas.width = container.clientWidth;
    canvas.height = container.clientHeight;
  }
  window.addEventListener('resize', resizeCanvas);
  resizeCanvas();

  const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
  function playMagicSound(type) {
    if (audioCtx.state === 'suspended') {
      audioCtx.resume();
    }
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.connect(gain);
    gain.connect(audioCtx.destination);

    const now = audioCtx.currentTime;
    if (type === 'type') {
      osc.type = 'sine';
      osc.frequency.setValueAtTime(440 + Math.random() * 200, now);
      gain.gain.setValueAtTime(0.15, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.08);
      osc.start(now);
      osc.stop(now + 0.08);
    } else if (type === 'explode') {
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(600, now);
      osc.frequency.exponentialRampToValueAtTime(150, now + 0.3);
      gain.gain.setValueAtTime(0.3, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.3);
      osc.start(now);
      osc.stop(now + 0.3);
    } else if (type === 'damage') {
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(180, now);
      osc.frequency.linearRampToValueAtTime(80, now + 0.25);
      gain.gain.setValueAtTime(0.3, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.25);
      osc.start(now);
      osc.stop(now + 0.25);
    } else if (type === 'potion' || type === 'levelup') {
      osc.type = 'sine';
      osc.frequency.setValueAtTime(520, now);
      osc.frequency.exponentialRampToValueAtTime(980, now + 0.35);
      gain.gain.setValueAtTime(0.3, now);
      gain.gain.exponentialRampToValueAtTime(0.01, now + 0.35);
      osc.start(now);
      osc.stop(now + 0.35);
    }
  }

  class FallingBlock {
    constructor(word, styleOverride = null) {
      this.word = word;
      this.id = Math.random().toString(36).substr(2, 9);
      
      this.style = styleOverride || colorPalettes[Math.floor(Math.random() * colorPalettes.length)];
      
      const fontWidthEstimate = this.word.length * 22 + 32;
      this.width = Math.max(64, fontWidthEstimate);
      this.height = 50;
      
      this.x = Math.random() * (canvas.width - this.width - 20) + 10;
      this.y = -this.height;
      
      const config = diffConfigs[selectedDifficulty];
      let levelSpeedMult = 1.0;
      if (currentLevel === 2) levelSpeedMult = 1.35;
      else if (currentLevel === 3) levelSpeedMult = 1.70;
      else if (currentLevel >= 4) levelSpeedMult = 2.10;

      const speedBonus = isFreezeActive ? 0.4 : 1.0;
      this.speed = (config.baseSpeed * levelSpeedMult + Math.random() * 0.4) * speedBonus;
      
      this.typedCount = 0;
    }

    update() {
      const speedMult = isFreezeActive ? 0.4 : 1.0;
      this.y += this.speed * speedMult;

      if (Math.random() < 0.6) {
        trailParticles.push(new TrailParticle(
          this.x + Math.random() * this.width,
          this.y + this.height - 4,
          this.style.glow
        ));
      }
    }

    draw() {
      ctx.save();
      const isTarget = (targetBlock === this);

      if (this.style.isSpecial === 'life') {
        ctx.shadowColor = '#52b788';
        ctx.shadowBlur = 25;
      } else if (this.style.isSpecial === 'boom') {
        ctx.shadowColor = '#ff758f';
        ctx.shadowBlur = 25;
      } else {
        ctx.shadowColor = isTarget ? '#ffd700' : this.style.glow;
        ctx.shadowBlur = isTarget ? 20 : 10;
      }

      ctx.fillStyle = this.style.bg;
      ctx.strokeStyle = isTarget ? '#ffd700' : this.style.border;
      ctx.lineWidth = isTarget ? 4 : (this.style.isSpecial ? 3 : 2);

      ctx.beginPath();
      ctx.roundRect(this.x, this.y, this.width, this.height, 12);
      ctx.fill();
      ctx.stroke();

      ctx.fillStyle = 'rgba(255, 255, 255, 0.25)';
      ctx.beginPath();
      ctx.roundRect(this.x + 3, this.y + 3, this.width - 6, this.height / 2 - 2, [8, 8, 0, 0]);
      ctx.fill();

      if (this.style.isSpecial === 'life') {
        ctx.font = '16px sans-serif';
        ctx.fillText('❤️', this.x + 12, this.y + 16);
      } else if (this.style.isSpecial === 'boom') {
        ctx.font = '16px sans-serif';
        ctx.fillText('💣', this.x + 12, this.y + 16);
      }

      ctx.shadowBlur = 0;
      ctx.font = '900 18px "Outfit", sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';

      const centerX = this.x + this.width / 2;
      const centerY = this.y + this.height / 2;

      if (isTarget && this.typedCount > 0) {
        const typedPart = this.word.substring(0, this.typedCount);
        const untypedPart = this.word.substring(this.typedCount);

        ctx.textAlign = 'left';
        const fullWidth = ctx.measureText(this.word).width;
        let startX = centerX - fullWidth / 2;

        ctx.fillStyle = '#ffd700';
        ctx.fillText(typedPart, startX, centerY);
        startX += ctx.measureText(typedPart).width;

        ctx.fillStyle = '#ffffff';
        ctx.fillText(untypedPart, startX, centerY);
      } else {
        ctx.fillStyle = '#ffffff';
        ctx.fillText(this.word, centerX, centerY);
      }

      ctx.restore();
    }
  }

  class TrailParticle {
    constructor(x, y, color) {
      this.x = x;
      this.y = y;
      this.vx = (Math.random() - 0.5) * 0.8;
      this.vy = -Math.random() * 1.2 - 0.5;
      this.size = Math.random() * 3 + 2;
      this.color = color;
      this.alpha = 0.9;
      this.decay = Math.random() * 0.03 + 0.02;
    }

    update() {
      this.x += this.vx;
      this.y += this.vy;
      this.alpha -= this.decay;
      this.size *= 0.96;
    }

    draw() {
      if (this.alpha <= 0) return;
      ctx.save();
      ctx.globalAlpha = this.alpha;
      ctx.fillStyle = this.color;
      ctx.shadowColor = this.color;
      ctx.shadowBlur = 6;
      ctx.beginPath();
      ctx.arc(this.x, this.y, Math.max(0.5, this.size), 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    }
  }

  class SparkParticle {
    constructor(x, y, color) {
      this.x = x;
      this.y = y;
      const angle = Math.random() * Math.PI * 2;
      const speed = Math.random() * 8 + 2;
      this.vx = Math.cos(angle) * speed;
      this.vy = Math.sin(angle) * speed;
      this.gravity = 0.15;
      this.friction = 0.96;
      this.size = Math.random() * 4 + 2;
      this.color = color;
      this.alpha = 1.0;
      this.decay = Math.random() * 0.03 + 0.015;
    }

    update() {
      this.vx *= this.friction;
      this.vy *= this.friction;
      this.vy += this.gravity;
      this.x += this.vx;
      this.y += this.vy;
      this.alpha -= this.decay;
    }

    draw() {
      if (this.alpha <= 0) return;
      ctx.save();
      ctx.globalAlpha = this.alpha;
      ctx.fillStyle = this.color;
      ctx.shadowColor = this.color;
      ctx.shadowBlur = 8;
      ctx.beginPath();
      ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    }
  }

  function explodeBlock(block, triggerBoomClearAll = true) {
    playMagicSound('explode');
    const centerX = block.x + block.width / 2;
    const centerY = block.y + block.height / 2;

    for (let i = 0; i < 35; i++) {
      sparkParticles.push(new SparkParticle(centerX, centerY, block.style.border));
      sparkParticles.push(new SparkParticle(centerX, centerY, '#ffd700'));
    }

    if (block.style.isSpecial === 'life') {
      const healAmount = (block.word === 'VIDA+2') ? 2 : 1;
      hp = Math.min(maxHp, hp + healAmount);
      for (let k = 0; k < 12; k++) {
        sparkParticles.push(new SparkParticle(centerX, centerY, '#52b788'));
      }
    } else if (block.style.isSpecial === 'boom' && triggerBoomClearAll) {
      // BOOM block clears ALL blocks on screen!
      const copyBlocks = [...blocks];
      blocks = [];
      targetBlock = null;
      copyBlocks.forEach(other => {
        if (other !== block) {
          explodeBlock(other, false);
        }
      });
    }

    score += 10;
    const config = diffConfigs[selectedDifficulty];
    const earnedCoins = Math.round(block.word.length * config.coinMult);
    coins += earnedCoins;

    checkLevelProgression();
    updateHUD();

    if (targetBlock === block) {
      targetBlock = null;
      updateTargetBar();
    }
  }

  function checkLevelProgression() {
    let oldLevel = currentLevel;
    if (score >= 600) {
      currentLevel = 4;
    } else if (score >= 300) {
      currentLevel = 3;
    } else if (score >= 100) {
      currentLevel = 2;
    } else {
      currentLevel = 1;
    }

    if (currentLevel > oldLevel) {
      playMagicSound('levelup');
      levelupText.textContent = `⚡ ¡NIVEL ${currentLevel} ALCANZADO! ⚡`;
      levelupBanner.classList.remove('hidden');
      setTimeout(() => {
        levelupBanner.classList.add('hidden');
      }, 2200);
    }
  }

  function spawnBlock() {
    const rand = Math.random();
    if (rand < 0.05) {
      const word = (score >= 300 && Math.random() < 0.4) ? 'VIDA+2' : 'VIDA';
      blocks.push(new FallingBlock(word, specialLifeStyle));
    } else if (rand < 0.09) {
      blocks.push(new FallingBlock('BOOM', specialBoomStyle));
    } else {
      const word = wordBank[Math.floor(Math.random() * wordBank.length)];
      blocks.push(new FallingBlock(word));
    }
  }

  function gameLoop(timestamp) {
    if (!gameRunning) return;

    if (!gamePaused) {
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      const activeBgImg = bgImages[selectedBgKey];
      if (activeBgImg && activeBgImg.complete && activeBgImg.naturalWidth !== 0) {
        ctx.drawImage(activeBgImg, 0, 0, canvas.width, canvas.height);
      } else {
        ctx.fillStyle = '#0f081d';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
      }

      const gradient = ctx.createRadialGradient(
        canvas.width / 2, canvas.height / 2, canvas.width * 0.3,
        canvas.width / 2, canvas.height / 2, canvas.width * 0.8
      );
      gradient.addColorStop(0, 'rgba(0,0,0,0.1)');
      gradient.addColorStop(1, 'rgba(0,0,0,0.65)');
      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      for (let i = trailParticles.length - 1; i >= 0; i--) {
        trailParticles[i].update();
        trailParticles[i].draw();
        if (trailParticles[i].alpha <= 0) {
          trailParticles.splice(i, 1);
        }
      }

      spawnTimer++;
      const currentSpawnInterval = diffConfigs[selectedDifficulty].spawnInterval;
      if (spawnTimer >= currentSpawnInterval) {
        spawnBlock();
        spawnTimer = 0;
      }

      const keyboardTopY = canvas.height - 180;
      for (let i = blocks.length - 1; i >= 0; i--) {
        const block = blocks[i];
        block.update();
        block.draw();

        if (block.y + block.height >= keyboardTopY) {
          playMagicSound('damage');
          hp--;
          updateHUD();

          for (let k = 0; k < 15; k++) {
            sparkParticles.push(new SparkParticle(block.x + block.width / 2, keyboardTopY, '#ff0000'));
          }

          if (targetBlock === block) {
            targetBlock = null;
            updateTargetBar();
          }

          blocks.splice(i, 1);

          if (hp <= 0) {
            gameOver();
            break;
          }
        }
      }

      for (let i = sparkParticles.length - 1; i >= 0; i--) {
        sparkParticles[i].update();
        sparkParticles[i].draw();
        if (sparkParticles[i].alpha <= 0) {
          sparkParticles.splice(i, 1);
        }
      }
    }

    requestAnimationFrame(gameLoop);
  }

  function handleKeyPress(char) {
    if (!gameRunning || gamePaused) return;

    playMagicSound('type');
    const upperChar = char.toUpperCase();

    if (upperChar === 'BACK') {
      if (targetBlock && targetBlock.typedCount > 0) {
        targetBlock.typedCount--;
        updateTargetBar();
      }
      return;
    }

    if (upperChar === 'CLEAR') {
      if (targetBlock) {
        targetBlock.typedCount = 0;
        targetBlock = null;
        updateTargetBar();
      }
      return;
    }

    if (!targetBlock) {
      let candidates = blocks.filter(b => b.word.startsWith(upperChar));
      if (candidates.length > 0) {
        candidates.sort((a, b) => b.y - a.y);
        targetBlock = candidates[0];
        targetBlock.typedCount = 1;
        updateTargetBar();

        if (targetBlock.typedCount >= targetBlock.word.length) {
          const idx = blocks.indexOf(targetBlock);
          if (idx !== -1) {
            explodeBlock(targetBlock);
            blocks.splice(idx, 1);
          }
        }
      }
    } else {
      const expectedChar = targetBlock.word[targetBlock.typedCount];
      if (upperChar === expectedChar) {
        targetBlock.typedCount++;
        updateTargetBar();

        if (targetBlock.typedCount >= targetBlock.word.length) {
          const idx = blocks.indexOf(targetBlock);
          if (idx !== -1) {
            explodeBlock(targetBlock);
            blocks.splice(idx, 1);
          }
        }
      } else {
        targetWordDisplay.style.color = '#ff4d4d';
        setTimeout(() => { targetWordDisplay.style.color = '#ffd700'; }, 200);
      }
    }
  }

  window.addEventListener('keydown', (e) => {
    if (e.key === 'Backspace') {
      handleKeyPress('BACK');
    } else if (e.key === 'Escape') {
      togglePause();
    } else if (e.key.length === 1 && e.key.match(/[a-zA-ZñÑ]/)) {
      handleKeyPress(e.key);
    }
  });

  document.querySelectorAll('.kb-key').forEach(keyBtn => {
    keyBtn.addEventListener('click', (e) => {
      e.preventDefault();
      const keyVal = keyBtn.getAttribute('data-key');
      keyBtn.classList.add('active-tap');
      setTimeout(() => keyBtn.classList.remove('active-tap'), 120);
      handleKeyPress(keyVal);
    });
  });

  canvas.addEventListener('click', (e) => {
    if (!gameRunning || gamePaused) return;

    const rect = canvas.getBoundingClientRect();
    const clickX = e.clientX - rect.left;
    const clickY = e.clientY - rect.top;

    for (let i = blocks.length - 1; i >= 0; i--) {
      const b = blocks[i];
      if (clickX >= b.x && clickX <= b.x + b.width && clickY >= b.y && clickY <= b.y + b.height) {
        targetBlock = b;
        updateTargetBar();
        playMagicSound('type');
        break;
      }
    }
  });

  function updateHUD() {
    coinsCountEl.textContent = coins;
    scoreCountEl.textContent = score;
    levelCountEl.textContent = currentLevel;

    heartsContainer.innerHTML = '';
    for (let i = 0; i < maxHp; i++) {
      const span = document.createElement('span');
      span.className = 'heart' + (i < hp ? ' active' : ' lost');
      span.textContent = '❤️';
      heartsContainer.appendChild(span);
    }

    countHealEl.textContent = inventory.heal;
    countFreezeEl.textContent = inventory.freeze;
    countNukeEl.textContent = inventory.nuke;
  }

  function updateTargetBar() {
    if (targetBlock) {
      wordTargetBar.classList.remove('hidden');
      const typed = targetBlock.word.substring(0, targetBlock.typedCount);
      const remaining = targetBlock.word.substring(targetBlock.typedCount);
      targetWordDisplay.innerHTML = `<span style="color:#ffd700">${typed}</span><span style="color:#ffffff">${remaining}</span>`;
    } else {
      wordTargetBar.classList.add('hidden');
    }
  }

  btnUseHeal.addEventListener('click', () => {
    if (!gameRunning || gamePaused) return;
    if (inventory.heal <= 0) {
      alert('¡No tienes pociones de salud! Cómpralas en la Tienda (Shop).');
      return;
    }
    if (hp >= maxHp) {
      alert('¡Tu salud ya está al máximo!');
      return;
    }
    inventory.heal--;
    hp++;
    playMagicSound('potion');
    updateHUD();
  });

  btnUseFreeze.addEventListener('click', () => {
    if (!gameRunning || gamePaused) return;
    if (inventory.freeze <= 0) {
      alert('¡No tienes pociones de hielo! Cómpralas en la Tienda (Shop).');
      return;
    }
    inventory.freeze--;
    playMagicSound('potion');
    isFreezeActive = true;
    updateHUD();
    if (freezeTimer) clearTimeout(freezeTimer);
    freezeTimer = setTimeout(() => {
      isFreezeActive = false;
    }, 8000);
  });

  btnUseNuke.addEventListener('click', () => {
    if (!gameRunning || gamePaused) return;
    if (inventory.nuke <= 0) {
      alert('¡No tienes bombas arcanas! Cómpralas en la Tienda (Shop).');
      return;
    }
    if (blocks.length === 0) return;

    inventory.nuke--;
    playMagicSound('potion');
    updateHUD();

    const copyBlocks = [...blocks];
    blocks = [];
    copyBlocks.forEach(b => explodeBlock(b, false));
  });

  function startGame() {
    hp = 3;
    maxHp = 3;
    score = 0;
    coins = 0;
    currentLevel = 1;
    blocks = [];
    trailParticles = [];
    sparkParticles = [];
    targetBlock = null;
    spawnTimer = 0;

    gameRunning = true;
    gamePaused = false;

    updateHUD();
    updateTargetBar();

    mainMenuOverlay.classList.add('hidden');
    gameoverModal.classList.add('hidden');
    settingsModal.classList.add('hidden');
    shopModal.classList.add('hidden');
    levelupBanner.classList.add('hidden');

    hud.classList.remove('hidden');
    potionBar.classList.remove('hidden');
    transparentKeyboard.classList.remove('hidden');

    requestAnimationFrame(gameLoop);
  }

  function togglePause() {
    if (!gameRunning) return;
    gamePaused = !gamePaused;
    if (gamePaused) {
      mainMenuOverlay.classList.remove('hidden');
      document.querySelector('.game-title').textContent = 'JUEGO EN PAUSA';
      document.querySelector('.game-subtitle').textContent = 'Elige una opción para continuar';
      btnPlay.querySelector('.btn-text').textContent = '▶️ CONTINUAR';
    } else {
      mainMenuOverlay.classList.add('hidden');
    }
  }

  function gameOver() {
    gameRunning = false;
    hud.classList.add('hidden');
    potionBar.classList.add('hidden');
    wordTargetBar.classList.add('hidden');
    transparentKeyboard.classList.add('hidden');

    document.getElementById('go-score').textContent = score;
    document.getElementById('go-coins').textContent = coins;
    gameoverModal.classList.remove('hidden');
  }

  btnPlay.addEventListener('click', () => {
    if (gamePaused) {
      gamePaused = false;
      mainMenuOverlay.classList.add('hidden');
    } else {
      startGame();
    }
  });

  btnSettings.addEventListener('click', () => {
    settingsModal.classList.remove('hidden');
  });

  btnCloseSettings.addEventListener('click', () => {
    settingsModal.classList.add('hidden');
  });

  btnShop.addEventListener('click', () => {
    shopCoinsVal.textContent = coins;
    shopMsg.textContent = '';
    shopModal.classList.remove('hidden');
  });

  btnCloseShop.addEventListener('click', () => {
    shopModal.classList.add('hidden');
  });

  btnPause.addEventListener('click', () => {
    togglePause();
  });

  btnRestart.addEventListener('click', () => {
    startGame();
  });

  btnMainMenu.addEventListener('click', () => {
    gameRunning = false;
    gameoverModal.classList.add('hidden');
    potionBar.classList.add('hidden');
    mainMenuOverlay.classList.remove('hidden');
    document.querySelector('.game-title').textContent = 'SPELL-MAGE';
    document.querySelector('.game-subtitle').textContent = 'Escribe los hechizos antes de que caigan';
    btnPlay.querySelector('.btn-text').textContent = '✨ PLAY ✨';
  });

  document.querySelectorAll('.diff-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.diff-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      selectedDifficulty = btn.getAttribute('data-diff');
    });
  });

  // Background selection listener for 4 options
  document.querySelectorAll('.bg-select-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.bg-select-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      selectedBgKey = btn.getAttribute('data-bg');
    });
  });

  document.querySelectorAll('.buy-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      const item = btn.getAttribute('data-item');
      const cost = parseInt(btn.getAttribute('data-cost'), 10);

      if (coins < cost) {
        shopMsg.style.color = '#ff4d4d';
        shopMsg.textContent = '❌ ¡No tienes suficientes monedas!';
        return;
      }

      if (item === 'max_hp') {
        coins -= cost;
        maxHp++;
        hp = Math.min(hp + 1, maxHp);
        updateHUD();
        shopCoinsVal.textContent = coins;
        shopMsg.style.color = '#7dbb42';
        shopMsg.textContent = `💖 ¡Salud máxima aumentada a ${maxHp} HP!`;
      } else if (item === 'heal_hp') {
        coins -= cost;
        inventory.heal++;
        updateHUD();
        shopCoinsVal.textContent = coins;
        shopMsg.style.color = '#7dbb42';
        shopMsg.textContent = '❤️ ¡Poción de salud comprada!';
      } else if (item === 'freeze') {
        coins -= cost;
        inventory.freeze++;
        updateHUD();
        shopCoinsVal.textContent = coins;
        shopMsg.style.color = '#7dbb42';
        shopMsg.textContent = '❄️ ¡Poción de hielo guardada!';
      } else if (item === 'nuke') {
        coins -= cost;
        inventory.nuke++;
        updateHUD();
        shopCoinsVal.textContent = coins;
        shopMsg.style.color = '#7dbb42';
        shopMsg.textContent = '💥 ¡Bomba Arcana guardada!';
      }
    });
  });

});
