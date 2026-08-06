import {expect, test} from '@playwright/test';

const task = {
    id: 'task-1',
    title: 'EchoPartition Algorithm Implementation',
    description: 'Rearrange an array around a pivot while preserving order.',
    constraints: ['The pivot may be absent from the input.'],
    testCases: [{input: '5\\n4 2 5 2 3', expectedOutput: '2 2 4 5 3'}],
    status: 'PUBLISHED',
};

test.beforeEach(async ({page}) => {
    await page.route('**/api/tasks', async route => {
        if (route.request().method() === 'GET') {
            await route.fulfill({json: [task]});
            return;
        }
        await route.continue();
    });
});

test('home_displaysPublishedTasks', async ({page}) => {
    await page.goto('/');

    await expect(page.getByRole('heading', {name: 'Choose a challenge'})).toBeVisible();
    await expect(page.getByRole('heading', {name: task.title})).toBeVisible();
    await expect(page.getByText('1 problems')).toBeVisible();
});

test('home_createTaskLink_opensCreationPage', async ({page}) => {
    await page.goto('/');

    await page.getByRole('link', {name: /Create task/}).click();

    await expect(page.getByRole('heading', {name: 'Create a challenge'})).toBeVisible();
    await expect(page.getByPlaceholder(/sliding-window problem/)).toBeVisible();
});

test('createTask_validGeneration_opensGeneratedTask', async ({page}) => {
    await page.route('**/api/tasks/generate', async route => {
        await route.fulfill({
            json: {
                status: 'GENERATED',
                task,
                similarTasks: [],
                warnings: [],
            },
        });
    });
    await page.route('**/api/tasks/task-1', async route => {
        await route.fulfill({json: task});
    });
    await page.route('**/api/tasks/task-1/solutions**', async route => {
        await route.fulfill({json: []});
    });

    await page.goto('/tasks/create');
    await page.getByPlaceholder(/sliding-window problem/).fill('stable partition around a pivot');
    await page.getByRole('button', {name: 'Generate task'}).click();

    await expect(page).toHaveURL(/\/tasks\/task-1$/);
    await expect(page.getByRole('heading', {name: task.title})).toBeVisible();
});
