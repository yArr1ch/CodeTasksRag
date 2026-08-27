module.exports = {
    extends: ['@commitlint/config-conventional'],
    rules: {
        'header-max-length': [2, 'always', 72],
        'subject-empty': [2, 'never'],
        'subject-full-stop': [2, 'never', '.'],
    },
};
