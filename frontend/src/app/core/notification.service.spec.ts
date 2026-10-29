import { OverlayContainer } from '@angular/cdk/overlay';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { NotificationService } from './notification.service';

@Component({ template: '' })
class NotificationHost {}

describe('Notifications', () => {
  function setup() {
    const fixture = TestBed.createComponent(NotificationHost);
    fixture.detectChanges();
    return { fixture, notifications: TestBed.inject(NotificationService),
      loader: TestbedHarnessEnvironment.documentRootLoader(fixture),
      overlay: TestBed.inject(OverlayContainer).getContainerElement() };
  }

  it('shows a success toast at the top right with a dismiss action', async () => {
    const { fixture, notifications, loader, overlay } = setup();
    notifications.success('Employee created.');
    fixture.detectChanges();
    const toast = await loader.getHarness(MatSnackBarHarness);
    expect(await toast.getMessage()).toBe('Success: Employee created.');
    expect(await toast.getActionDescription()).toBe('Dismiss');
    expect(await toast.getAriaLive()).toBe('polite');
    expect(overlay.querySelector('.toast-success')).not.toBeNull();
    const position = overlay.querySelector<HTMLElement>('.cdk-global-overlay-wrapper');
    expect(position?.style.justifyContent).toBe('flex-end');
    expect(position?.style.alignItems).toBe('flex-start');
    await toast.dismissWithAction();
    expect(await toast.isDismissed()).toBe(true);
  });

  it('shows an error with an accessible text label and assertive announcement', async () => {
    const { fixture, notifications, loader, overlay } = setup();
    notifications.error('Could not save employee.');
    fixture.detectChanges();
    const toast = await loader.getHarness(MatSnackBarHarness);
    expect(await toast.getMessage()).toBe('Error: Could not save employee.');
    expect(await toast.getAriaLive()).toBe('assertive');
    expect(overlay.querySelector('.toast-error')).not.toBeNull();
  });
});
