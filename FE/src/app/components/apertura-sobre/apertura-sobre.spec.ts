import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AperturaSobre } from './apertura-sobre';

describe('AperturaSobre', () => {
  let component: AperturaSobre;
  let fixture: ComponentFixture<AperturaSobre>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AperturaSobre],
    }).compileComponents();

    fixture = TestBed.createComponent(AperturaSobre);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
