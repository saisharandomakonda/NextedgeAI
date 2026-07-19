import { Icon, Switch, Tooltip } from 'antd';

import InputWithLabel from 'components/inputs/InputWithLabel';
import { HStack } from 'components/layout';
import { tNamespaced } from 'utils/i18nUtil';
import { colors, variables } from 'utils/LessConstants';

import { usePipelineSettings } from '../settings/Settings.hooks';
import { useRealtimePipelineContext } from './RealtimePipeline.context';

import './RealtimePipelineToggle.scss';

const tn = tNamespaced('RealtimePipeline');

const realtimePipelineHelp = 'https://support.syncari.com/hc/en-us/articles/32688129149588-Realtime-Pipeline';

export const RealtimePipelineToggle = () => {
  const { version } = usePipelineSettings();

  const { setVisible, enabled, setDisabledVisible } = useRealtimePipelineContext();

  return (
    <HStack spacing="sm">
      <InputWithLabel
        className="realtime-pipeline-toggle"
        label={tn('realtime_pipeline')}
        name="realtimePipeline"
        input={
          <HStack spacing="z" className="realtime-pipeline-toggle__switch">
            <Tooltip
              title={
                <span
                  className="realtime-pipeline-toggle__help_tooltip"
                  dangerouslySetInnerHTML={{ __html: tn('enable_tooltip', { link: realtimePipelineHelp }) }}
                />
              }>
              <Icon
                type="question-circle"
                theme="filled"
                style={{ fontSize: variables.fontSizes.lgr, color: colors.gray700 }}
              />
            </Tooltip>
            <Switch
              checkedChildren={<Icon type="check" />}
              unCheckedChildren={<Icon type="close" />}
              disabled={version !== 'NEW'}
              checked={enabled}
              onChange={(value: boolean) => {
                if (value) {
                  setVisible(true);
                } else if (enabled) {
                  setDisabledVisible(true);
                }
              }}
            />
          </HStack>
        }
      />
      {enabled && (
        <Icon
          className="realtime-pipeline-toggle__link"
          type="setting"
          theme="filled"
          style={{ color: 'black', fontSize: 24, cursor: 'pointer' }}
          onClick={() => {
            setVisible(true);
          }}
        />
      )}
    </HStack>
  );
};
