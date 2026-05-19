import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AperturaSobreComponent } from './apertura-sobre';

// Verifica que el componente de apertura pueda montarse.
describe('AperturaSobre', () => {
  let component: AperturaSobreComponent;
  let fixture: ComponentFixture<AperturaSobreComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AperturaSobreComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(AperturaSobreComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
