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
    await page.route('**/api/tasks*', async route => {
        if (route.request().method() === 'GET') {
            await route.fulfill({json: {tasks: [task], nextCursor: null, hasMore: false}});
            return;
        }
        await route.continue();
    });
});

test('home_displaysPublishedTasks', async ({page}) => {
    await page.goto('/');

    await expect(page.getByRole('heading', {name: 'Choose a challenge'})).toBeVisible();
    await expect(page.getByRole('heading', {name: task.title})).toBeVisible();
    await expect(page.getByText('1 loaded')).toBeVisible();
});

test('home_loadsNextTaskPage_whenFeedReachesSentinel', async ({page}) => {
    await page.unroute('**/api/tasks*');
    const nextTask = {...task, id: 'task-2', title: 'BeaconWindow Algorithm Implementation'};
    await page.route('**/api/tasks*', async route => {
        const requestUrl = new URL(route.request().url());
        await route.fulfill({json: requestUrl.searchParams.has('cursor')
                ? {tasks: [nextTask], nextCursor: null, hasMore: false}
                : {tasks: [task], nextCursor: 'cursor-1', hasMore: true}});
    });

    await page.goto('/');
    await expect(page.getByRole('heading', {name: task.title})).toBeVisible();
    await page.locator('.task-feed-status').scrollIntoViewIfNeeded();
    await expect(page.getByRole('heading', {name: nextTask.title})).toBeVisible();
    await expect(page.getByText('2 loaded')).toBeVisible();
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
                id: 'generation-1',
                prompt: 'stable partition around a pivot',
                status: 'GENERATING',
                attempt: 1,
                maxAttempts: 3,
                similarTasks: [],
            },
        });
    });
    await page.route('**/api/tasks/generations/generation-1', async route => {
        await route.fulfill({
            json: {
                id: 'generation-1',
                prompt: 'stable partition around a pivot',
                status: 'READY',
                attempt: 1,
                maxAttempts: 3,
                taskId: task.id,
                similarTasks: [],
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

test('createTask_failedGeneration_canRetryWithoutRetypingPrompt', async ({page}) => {
    let requestCount = 0;
    await page.route('**/api/tasks/generate', async route => {
        requestCount += 1;
        await route.fulfill({
            json: {
                id: `generation-${requestCount}`,
                prompt: 'stable partition around a pivot',
                status: 'GENERATING',
                attempt: 0,
                maxAttempts: 3,
                similarTasks: [],
            },
        });
    });
    await page.route('**/api/tasks/generations/generation-*', async route => {
        const generationId = route.request().url().split('/').pop();
        await route.fulfill({
            json: generationId === 'generation-1'
                ? {
                    id: generationId,
                    prompt: 'stable partition around a pivot',
                    status: 'FAILED',
                    attempt: 3,
                    maxAttempts: 3,
                    similarTasks: [],
                    error: 'Reference solution failed deterministic execution',
                }
                : {
                    id: generationId,
                    prompt: 'stable partition around a pivot',
                    status: 'READY',
                    attempt: 1,
                    maxAttempts: 3,
                    taskId: task.id,
                    similarTasks: [],
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

    await expect(page.getByText('Reference solution failed deterministic execution')).toBeVisible();
    await page.getByRole('button', {name: 'Retry generation'}).click();

    await expect(page).toHaveURL(/\/tasks\/task-1$/);
    expect(requestCount).toBe(2);
});
