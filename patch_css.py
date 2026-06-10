import sys
content = open('frontend/src/app/features/battle/battle-board.component.scss', 'r', encoding='utf-8').read()

css = '''
/* SETUP MULLIGAN OVERLAY */
.setup-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: 4000;
  display: flex;
  align-items: center;
  justify-content: center;
  
  .setup-backdrop {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.85);
    backdrop-filter: blur(8px);
  }

  .setup-panel {
    position: relative;
    z-index: 2;
    background: linear-gradient(145deg, #1e293b, #0f172a);
    border: 2px solid rgba(255, 255, 255, 0.1);
    border-radius: 24px;
    padding: 3rem;
    max-width: 900px;
    width: 90%;
    text-align: center;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.05) inset;
    animation: setupPanelIn 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;

    .setup-title {
      font-size: 2.5rem;
      font-weight: 900;
      color: #fff;
      margin: 0 0 0.5rem;
      text-transform: uppercase;
      letter-spacing: 2px;
      text-shadow: 0 2px 10px rgba(0, 0, 0, 0.5);
    }

    .setup-sub {
      color: #94a3b8;
      font-size: 1.1rem;
      margin-bottom: 2rem;
    }

    .setup-hand-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
      justify-content: center;
      margin-top: 2rem;
      
      .setup-card {
        width: 120px;
        height: 168px;
        border-radius: 8px;
        overflow: hidden;
        box-shadow: 0 10px 20px rgba(0,0,0,0.4);
        transform: translateY(20px);
        opacity: 0;
        animation: cardFloatUp 0.6s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;

        @for $i from 1 through 10 {
          &:nth-child(#{$i}) {
            animation-delay: #{$i * 0.1}s;
          }
        }

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }
    }

    .setup-draw-actions {
      display: flex;
      gap: 1.5rem;
      justify-content: center;
      margin-top: 2rem;

      button {
        padding: 1rem 2.5rem;
        font-size: 1.1rem;
        font-weight: 700;
        border-radius: 12px;
        cursor: pointer;
        transition: all 0.2s ease;
        text-transform: uppercase;
        letter-spacing: 1px;

        &.btn-primary {
          background: linear-gradient(135deg, #3b82f6, #2563eb);
          color: white;
          border: none;
          box-shadow: 0 4px 15px rgba(37, 99, 235, 0.4);

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 25px rgba(37, 99, 235, 0.6);
          }
        }

        &.btn-secondary {
          background: rgba(255, 255, 255, 0.1);
          color: #e2e8f0;
          border: 1px solid rgba(255, 255, 255, 0.2);

          &:hover {
            background: rgba(255, 255, 255, 0.15);
            transform: translateY(-2px);
          }
        }
      }
    }
  }
}

@keyframes setupPanelIn {
  from { opacity: 0; transform: scale(0.9) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

@keyframes cardFloatUp {
  from { opacity: 0; transform: translateY(40px) scale(0.8); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
'''

with open('frontend/src/app/features/battle/battle-board.component.scss', 'w', encoding='utf-8') as f:
    f.write(content + "\n" + css)
print('CSS appended successfully.')
